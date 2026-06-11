package com.webtoonhub.auth.service;

import com.webtoonhub.auth.dto.EmailVerificationResponse;
import com.webtoonhub.common.exception.BadRequestException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    private static final String SIGNUP_PURPOSE = "SIGNUP";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[0-9]{6}$");

    private final NamedParameterJdbcTemplate jdbc;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();
    private final String mailHost;
    private final String fromAddress;
    private final byte[] secretBytes;
    private final Duration codeValidity;
    private final int maxAttempts;

    public EmailVerificationService(
        NamedParameterJdbcTemplate jdbc,
        JavaMailSender mailSender,
        @Value("${spring.mail.host:}") String mailHost,
        @Value("${app.auth.email-verification.from:no-reply@tooniping.app}") String fromAddress,
        @Value("${app.auth.secret:webtoon-hub-secret-key-change-this}") String authSecret,
        @Value("${app.auth.email-verification.expire-minutes:10}") long expireMinutes,
        @Value("${app.auth.email-verification.max-attempts:5}") int maxAttempts
    ) {
        this.jdbc = jdbc;
        this.mailSender = mailSender;
        this.mailHost = mailHost == null ? "" : mailHost.trim();
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
        this.secretBytes = authSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        this.codeValidity = Duration.ofMinutes(Math.max(expireMinutes, 1L));
        this.maxAttempts = Math.max(maxAttempts, 1);
    }

    @Transactional
    public EmailVerificationResponse sendSignupCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (emailExists(normalizedEmail)) {
            throw new BadRequestException("이미 사용 중인 이메일입니다.");
        }
        if (mailHost.isBlank()) {
            throw new BadRequestException("이메일 발송 설정이 필요합니다.");
        }

        String code = generateCode();
        Instant expiresAt = Instant.now().plus(codeValidity);

        jdbc.update("""
            DELETE FROM email_verification_codes
            WHERE email = :email
              AND purpose = :purpose
            """, new MapSqlParameterSource()
            .addValue("email", normalizedEmail)
            .addValue("purpose", SIGNUP_PURPOSE));

        jdbc.update("""
            INSERT INTO email_verification_codes (
              email,
              purpose,
              code_hash,
              attempt_count,
              expires_at,
              created_at
            )
            VALUES (
              :email,
              :purpose,
              :codeHash,
              0,
              :expiresAt,
              CURRENT_TIMESTAMP
            )
            """, new MapSqlParameterSource()
            .addValue("email", normalizedEmail)
            .addValue("purpose", SIGNUP_PURPOSE)
            .addValue("codeHash", hashCode(normalizedEmail, code))
            .addValue("expiresAt", Timestamp.from(expiresAt)));

        sendMail(normalizedEmail, code, expiresAt);
        return new EmailVerificationResponse(normalizedEmail, expiresAt.toString(), false);
    }

    @Transactional
    public EmailVerificationResponse verifySignupCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = normalizeCode(code);
        VerificationRow row = findLatestCode(normalizedEmail);
        Instant now = Instant.now();

        if (row == null) {
            throw new BadRequestException("인증번호를 먼저 요청해 주세요.");
        }
        if (row.expiresAt().isBefore(now)) {
            throw new BadRequestException("인증번호가 만료되었습니다. 다시 요청해 주세요.");
        }
        if (row.attemptCount() >= maxAttempts) {
            throw new BadRequestException("인증번호 입력 횟수를 초과했습니다. 다시 요청해 주세요.");
        }

        boolean matches = MessageDigest.isEqual(
            row.codeHash().getBytes(java.nio.charset.StandardCharsets.UTF_8),
            hashCode(normalizedEmail, normalizedCode).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        int nextAttemptCount = row.attemptCount() + 1;

        if (!matches) {
            jdbc.update("""
                UPDATE email_verification_codes
                SET attempt_count = :attemptCount
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("attemptCount", nextAttemptCount)
                .addValue("id", row.id()));
            throw new BadRequestException("인증번호가 올바르지 않습니다.");
        }

        jdbc.update("""
            UPDATE email_verification_codes
            SET attempt_count = :attemptCount,
                verified_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """, new MapSqlParameterSource()
            .addValue("attemptCount", nextAttemptCount)
            .addValue("id", row.id()));

        return new EmailVerificationResponse(normalizedEmail, row.expiresAt().toString(), true);
    }

    public void requireVerifiedSignupEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String sql = """
            SELECT COUNT(*)
            FROM email_verification_codes
            WHERE email = :email
              AND purpose = :purpose
              AND verified_at IS NOT NULL
              AND expires_at > CURRENT_TIMESTAMP
            """;
        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource()
            .addValue("email", normalizedEmail)
            .addValue("purpose", SIGNUP_PURPOSE), Long.class);
        if (count == null || count == 0) {
            throw new BadRequestException("이메일 인증을 완료해 주세요.");
        }
    }

    @Transactional
    public void consumeSignupEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        jdbc.update("""
            DELETE FROM email_verification_codes
            WHERE email = :email
              AND purpose = :purpose
            """, new MapSqlParameterSource()
            .addValue("email", normalizedEmail)
            .addValue("purpose", SIGNUP_PURPOSE));
    }

    private void sendMail(String email, String code, Instant expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (!fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(email);
        message.setSubject("[Tooniping] 이메일 인증번호");
        message.setText("""
            Tooniping 회원가입 이메일 인증번호입니다.

            인증번호: %s

            이 번호는 %d분 동안만 사용할 수 있습니다.
            본인이 요청하지 않았다면 이 메일을 무시해 주세요.
            """.formatted(code, codeValidity.toMinutes()));
        message.setSentDate(java.util.Date.from(Instant.now()));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new BadRequestException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private VerificationRow findLatestCode(String email) {
        String sql = """
            SELECT id, code_hash, attempt_count, expires_at
            FROM email_verification_codes
            WHERE email = :email
              AND purpose = :purpose
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """;
        List<VerificationRow> rows = jdbc.query(sql, new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("purpose", SIGNUP_PURPOSE),
            (rs, rowNum) -> new VerificationRow(
                rs.getLong("id"),
                rs.getString("code_hash"),
                rs.getInt("attempt_count"),
                rs.getTimestamp("expires_at").toInstant()
            ));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean emailExists(String email) {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM user_accounts WHERE username = :email)
              +
              (SELECT COUNT(*) FROM user_profiles WHERE username = :email)
            """;
        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource("email", email), Long.class);
        return count != null && count > 0;
    }

    private String generateCode() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }

    private String hashCode(String email, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(secretBytes);
            digest.update((email + ":" + code).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("인증번호 해시 처리 중 오류가 발생했습니다.", e);
        }
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BadRequestException("이메일을 입력해 주세요.");
        }
        if (normalized.length() > 100) {
            throw new BadRequestException("이메일은 100자 이하로 입력해 주세요.");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("올바른 이메일 형식이 아닙니다.");
        }
        return normalized;
    }

    private String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim();
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("인증번호 6자리를 입력해 주세요.");
        }
        return normalized;
    }

    private record VerificationRow(
        long id,
        String codeHash,
        int attemptCount,
        Instant expiresAt
    ) {
    }
}
