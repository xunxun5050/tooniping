package com.webtoonhub.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webtoonhub.auth.dto.LoginRequest;
import com.webtoonhub.auth.dto.LoginResponse;
import com.webtoonhub.auth.dto.UserProfileDto;
import com.webtoonhub.common.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final String JWT_HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;
    private final String configuredUsername;
    private final String configuredPassword;
    private final byte[] secretBytes;
    private final long tokenValidSeconds;

    public AuthService(
        ObjectMapper objectMapper,
        UserProfileService userProfileService,
        @Value("${app.auth.username:admin}") String configuredUsername,
        @Value("${app.auth.password:admin1234}") String configuredPassword,
        @Value("${app.auth.secret:webtoon-hub-secret-key-change-this}") String secret,
        @Value("${app.auth.token-valid-minutes:480}") long tokenValidMinutes
    ) {
        this.objectMapper = objectMapper;
        this.userProfileService = userProfileService;
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.tokenValidSeconds = Math.max(tokenValidMinutes, 1L) * 60L;
    }

    public LoginResponse login(LoginRequest request) {
        if (!configuredUsername.equals(request.username()) || !configuredPassword.equals(request.password())) {
            throw new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueLoginResponse(configuredUsername, "LOCAL", configuredUsername, null);
    }

    public LoginResponse loginWithUsername(String username) {
        return loginWithUsername(username, null, null, null);
    }

    public LoginResponse loginWithUsername(
        String username,
        String provider,
        String providerUserId,
        String sourceNickname
    ) {
        if (username == null || username.isBlank()) {
            throw new UnauthorizedException("유효하지 않은 사용자 정보입니다.");
        }
        return issueLoginResponse(username, provider, providerUserId, sourceNickname);
    }

    public UserProfileDto getProfile(String username) {
        return userProfileService.getOrCreateProfile(username);
    }

    public UserProfileDto updateNickname(String username, String nickname) {
        return userProfileService.updateNickname(username, nickname);
    }

    private LoginResponse issueLoginResponse(
        String username,
        String provider,
        String providerUserId,
        String sourceNickname
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(tokenValidSeconds);
        String token = issueToken(username, issuedAt, expiresAt);
        UserProfileDto profile = userProfileService.ensureProfile(username, provider, providerUserId, sourceNickname);

        return new LoginResponse(
            token,
            "Bearer",
            username,
            profile.nickname(),
            expiresAt.toString(),
            toWeekdayCode(issuedAt)
        );
    }

    public String authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return validateToken(token);
    }

    private String issueToken(String username, Instant issuedAt, Instant expiresAt) {
        String header = encodeUrl(JWT_HEADER_JSON.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> payloadMap = Map.of(
            "sub", username,
            "iat", issuedAt.getEpochSecond(),
            "exp", expiresAt.getEpochSecond()
        );
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            throw new IllegalStateException("토큰 생성 중 오류가 발생했습니다.", e);
        }

        String payload = encodeUrl(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        String signature = encodeUrl(sign(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + signature;
    }

    private String validateToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new UnauthorizedException("유효하지 않은 로그인 토큰입니다.");
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expectedSignature = sign(signingInput.getBytes(StandardCharsets.UTF_8));
        byte[] actualSignature = decodeUrl(parts[2]);

        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw new UnauthorizedException("유효하지 않은 로그인 토큰입니다.");
        }

        Map<String, Object> payload;
        try {
            byte[] payloadBytes = decodeUrl(parts[1]);
            payload = objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new UnauthorizedException("유효하지 않은 로그인 토큰입니다.");
        }

        String username = toStringValue(payload.get("sub"));
        long exp = toLongValue(payload.get("exp"));
        long nowEpoch = Instant.now().getEpochSecond();

        if (username.isBlank() || exp < nowEpoch) {
            throw new UnauthorizedException("로그인 토큰이 만료되었거나 유효하지 않습니다.");
        }

        return username;
    }

    private byte[] sign(byte[] data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            return hmac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("토큰 서명 처리 중 오류가 발생했습니다.", e);
        }
    }

    private String encodeUrl(byte[] value) {
        return URL_ENCODER.encodeToString(value);
    }

    private byte[] decodeUrl(String value) {
        try {
            return URL_DECODER.decode(value);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("유효하지 않은 로그인 토큰입니다.");
        }
    }

    private String toStringValue(Object value) {
        if (value instanceof String text) {
            return text;
        }
        return "";
    }

    private long toLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return -1L;
            }
        }
        return -1L;
    }

    private String toWeekdayCode(Instant instant) {
        DayOfWeek day = instant.atZone(ASIA_SEOUL).getDayOfWeek();
        return switch (day) {
            case MONDAY -> "MONDAY";
            case TUESDAY -> "TUESDAY";
            case WEDNESDAY -> "WEDNESDAY";
            case THURSDAY -> "THURSDAY";
            case FRIDAY -> "FRIDAY";
            case SATURDAY -> "SATURDAY";
            case SUNDAY -> "SUNDAY";
        };
    }
}
