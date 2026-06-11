package com.webtoonhub.auth.service;

import com.webtoonhub.auth.dto.SignupRequest;
import com.webtoonhub.auth.dto.UserProfileDto;
import com.webtoonhub.common.exception.BadRequestException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PASSWORD_LETTER_PATTERN = Pattern.compile("[A-Za-z]");
    private static final Pattern PASSWORD_NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int PASSWORD_KEY_LENGTH = 256;
    private static final int PASSWORD_SALT_BYTES = 16;
    private static final String PASSWORD_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_PREFIX = "pbkdf2_sha256";

    private final NamedParameterJdbcTemplate jdbc;
    private final UserProfileService userProfileService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserAccountService(NamedParameterJdbcTemplate jdbc, UserProfileService userProfileService) {
        this.jdbc = jdbc;
        this.userProfileService = userProfileService;
    }

    @Transactional
    public UserProfileDto signup(SignupRequest request, String reservedAdminUsername) {
        String email = normalizeEmail(request.email());
        String password = normalizePassword(request.password());
        if (email.equalsIgnoreCase(reservedAdminUsername)) {
            throw new BadRequestException("이미 사용 중인 이메일입니다.");
        }
        if (emailExists(email)) {
            throw new BadRequestException("이미 사용 중인 이메일입니다.");
        }

        String sql = """
            INSERT INTO user_accounts (username, password_hash, role, created_at, updated_at)
            VALUES (:username, :passwordHash, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
        try {
            jdbc.update(sql, new MapSqlParameterSource()
                .addValue("username", email)
                .addValue("passwordHash", hashPassword(password)));
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("이미 사용 중인 이메일입니다.");
        }

        return userProfileService.createLocalProfile(email, request.nickname());
    }

    public boolean verifyPassword(String email, String password) {
        String normalizedEmail = normalizeEmailForLogin(email);
        String sql = """
            SELECT password_hash
            FROM user_accounts
            WHERE username = :username
            """;
        List<String> hashes = jdbc.query(sql, new MapSqlParameterSource("username", normalizedEmail),
            (rs, rowNum) -> rs.getString("password_hash"));
        if (hashes.isEmpty()) {
            return false;
        }
        return matches(password == null ? "" : password, hashes.get(0));
    }

    public boolean isAdmin(String username) {
        String sql = """
            SELECT role
            FROM user_accounts
            WHERE username = :username
            """;
        List<String> roles = jdbc.query(sql, new MapSqlParameterSource("username", username),
            (rs, rowNum) -> rs.getString("role"));
        return !roles.isEmpty() && "ADMIN".equalsIgnoreCase(roles.get(0));
    }

    public void deleteAccount(String username) {
        jdbc.update("DELETE FROM user_accounts WHERE username = :username",
            new MapSqlParameterSource("username", username));
    }

    private boolean emailExists(String email) {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM user_accounts WHERE username = :username)
              +
              (SELECT COUNT(*) FROM user_profiles WHERE username = :username)
            """;
        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource("username", email), Long.class);
        return count != null && count > 0;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeEmailForLogin(email);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("올바른 이메일 형식이 아닙니다.");
        }
        return normalized;
    }

    private String normalizeEmailForLogin(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new BadRequestException("이메일을 입력해 주세요.");
        }
        if (normalized.length() > 100) {
            throw new BadRequestException("이메일은 100자 이하로 입력해 주세요.");
        }
        return normalized;
    }

    private String normalizePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new BadRequestException("비밀번호는 8~72자로 입력해 주세요.");
        }
        if (!PASSWORD_LETTER_PATTERN.matcher(password).find() || !PASSWORD_NUMBER_PATTERN.matcher(password).find()) {
            throw new BadRequestException("비밀번호는 영문과 숫자를 함께 입력해 주세요.");
        }
        return password;
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[PASSWORD_SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, PASSWORD_ITERATIONS, PASSWORD_KEY_LENGTH);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return HASH_PREFIX + "$" + PASSWORD_ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    private boolean matches(String password, String storedHash) {
        String[] parts = storedHash == null ? new String[0] : storedHash.split("\\$");
        if (parts.length != 4 || !HASH_PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expectedHash = decoder.decode(parts[3]);
            byte[] actualHash = pbkdf2(password, salt, iterations, expectedHash.length * 8);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLength) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            return SecretKeyFactory.getInstance(PASSWORD_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("비밀번호 해시 처리 중 오류가 발생했습니다.", e);
        }
    }
}
