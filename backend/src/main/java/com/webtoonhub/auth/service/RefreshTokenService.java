package com.webtoonhub.auth.service;

import com.webtoonhub.common.exception.UnauthorizedException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final NamedParameterJdbcTemplate jdbc;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration refreshTokenValidity;

    public RefreshTokenService(
        NamedParameterJdbcTemplate jdbc,
        @Value("${app.auth.refresh-token-valid-days:30}") long refreshTokenValidDays
    ) {
        this.jdbc = jdbc;
        this.refreshTokenValidity = Duration.ofDays(Math.max(refreshTokenValidDays, 1L));
    }

    @Transactional
    public RefreshToken issue(String username) {
        String token = generateToken();
        String tokenHash = hashToken(token);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTokenValidity);
        String sql = """
            INSERT INTO user_refresh_tokens (token_hash, username, expires_at, created_at, last_used_at)
            VALUES (:tokenHash, :username, :expiresAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("tokenHash", tokenHash)
            .addValue("username", username)
            .addValue("expiresAt", Timestamp.from(expiresAt)));
        return new RefreshToken(token, expiresAt);
    }

    @Transactional
    public TokenOwner consumeAndRotate(String token) {
        String tokenHash = hashToken(normalizeToken(token));
        String sql = """
            SELECT username, expires_at
            FROM user_refresh_tokens
            WHERE token_hash = :tokenHash
            """;
        List<TokenRow> rows = jdbc.query(sql, new MapSqlParameterSource("tokenHash", tokenHash),
            (rs, rowNum) -> new TokenRow(
                rs.getString("username"),
                rs.getTimestamp("expires_at").toInstant()
            ));

        if (rows.isEmpty()) {
            throw new UnauthorizedException("로그인 유지 정보가 유효하지 않습니다.");
        }

        TokenRow row = rows.get(0);
        jdbc.update("DELETE FROM user_refresh_tokens WHERE token_hash = :tokenHash",
            new MapSqlParameterSource("tokenHash", tokenHash));

        if (row.expiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("로그인 유지 정보가 만료되었습니다.");
        }

        return new TokenOwner(row.username(), issue(row.username()));
    }

    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        jdbc.update("DELETE FROM user_refresh_tokens WHERE token_hash = :tokenHash",
            new MapSqlParameterSource("tokenHash", hashToken(token)));
    }

    @Transactional
    public void revokeAllForUser(String username) {
        jdbc.update("DELETE FROM user_refresh_tokens WHERE username = :username",
            new MapSqlParameterSource("username", username));
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("로그인 유지 정보가 없습니다.");
        }
        return token.trim();
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(normalizeToken(token).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("토큰 해시 처리 중 오류가 발생했습니다.", e);
        }
    }

    public record RefreshToken(String value, Instant expiresAt) {
    }

    public record TokenOwner(String username, RefreshToken refreshToken) {
    }

    private record TokenRow(String username, Instant expiresAt) {
    }
}
