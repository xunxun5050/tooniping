package com.webtoonhub.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webtoonhub.auth.dto.LoginResponse;
import com.webtoonhub.common.exception.BadRequestException;
import com.webtoonhub.common.exception.UnauthorizedException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SocialAuthService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final String KAKAO_AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_ME_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String NAVER_AUTHORIZE_URL = "https://nid.naver.com/oauth2.0/authorize";
    private static final String NAVER_TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String NAVER_ME_URL = "https://openapi.naver.com/v1/nid/me";

    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final HttpClient httpClient;
    private final byte[] stateSecretBytes;
    private final String frontendBaseUrl;

    private final ProviderConfig kakaoConfig;
    private final ProviderConfig naverConfig;

    public SocialAuthService(
        ObjectMapper objectMapper,
        AuthService authService,
        @Value("${app.auth.secret:webtoon-hub-secret-key-change-this}") String authSecret,
        @Value("${app.auth.oauth.frontend-base-url:http://localhost:3000}") String frontendBaseUrl,
        @Value("${app.auth.oauth.kakao.client-id:}") String kakaoClientId,
        @Value("${app.auth.oauth.kakao.client-secret:}") String kakaoClientSecret,
        @Value("${app.auth.oauth.kakao.redirect-uri:http://localhost:8080/api/auth/oauth/kakao/callback}") String kakaoRedirectUri,
        @Value("${app.auth.oauth.naver.client-id:}") String naverClientId,
        @Value("${app.auth.oauth.naver.client-secret:}") String naverClientSecret,
        @Value("${app.auth.oauth.naver.redirect-uri:http://localhost:8080/api/auth/oauth/naver/callback}") String naverRedirectUri
    ) {
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.httpClient = HttpClient.newHttpClient();
        this.stateSecretBytes = authSecret.getBytes(StandardCharsets.UTF_8);
        this.frontendBaseUrl = trimTrailingSlash(frontendBaseUrl);
        this.kakaoConfig = new ProviderConfig(kakaoClientId, kakaoClientSecret, kakaoRedirectUri);
        this.naverConfig = new ProviderConfig(naverClientId, naverClientSecret, naverRedirectUri);
    }

    public String buildAuthorizeUrl(String providerText, String nextPath) {
        OAuthProvider provider = OAuthProvider.from(providerText);
        ProviderConfig config = getProviderConfig(provider);
        validateProviderConfig(provider, config);

        String safeNextPath = normalizeNextPath(nextPath);
        String state = createState(provider, safeNextPath);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", config.clientId());
        params.put("redirect_uri", config.redirectUri());
        params.put("state", state);

        if (provider == OAuthProvider.NAVER) {
            return NAVER_AUTHORIZE_URL + "?" + toQueryString(params);
        }
        return KAKAO_AUTHORIZE_URL + "?" + toQueryString(params);
    }

    public String handleCallback(String providerText, String code, String state, String error, String errorDescription) {
        OAuthProvider provider = OAuthProvider.from(providerText);
        if (hasText(error)) {
            String message = hasText(errorDescription) ? errorDescription : provider.displayName() + " 로그인에 실패했습니다.";
            return buildLoginErrorRedirect(message, "/mypage");
        }
        if (!hasText(code) || !hasText(state)) {
            return buildLoginErrorRedirect("소셜 로그인 인증 정보가 누락되었습니다.", "/mypage");
        }

        ProviderConfig config = getProviderConfig(provider);
        validateProviderConfig(provider, config);
        OAuthState oauthState = verifyState(provider, state);

        try {
            String accessToken = requestAccessToken(provider, config, code, state);
            SocialUserProfile profile = requestSocialProfile(provider, accessToken);
            String username = generateUsername(provider, profile.providerUserId());
            LoginResponse loginResponse = authService.loginWithUsername(
                username,
                provider.code().toUpperCase(),
                profile.providerUserId(),
                profile.nickname()
            );

            Map<String, String> successParams = new LinkedHashMap<>();
            successParams.put("token", loginResponse.token());
            successParams.put("tokenType", loginResponse.tokenType());
            successParams.put("username", loginResponse.username());
            successParams.put("nickname", loginResponse.nickname());
            successParams.put("expiresAt", loginResponse.expiresAt());
            successParams.put("loginWeekday", loginResponse.loginWeekday());
            successParams.put("next", oauthState.nextPath());

            return frontendBaseUrl + "/auth/callback?" + toQueryString(successParams);
        } catch (Exception e) {
            return buildLoginErrorRedirect(provider.displayName() + " 로그인 처리 중 오류가 발생했습니다.", oauthState.nextPath());
        }
    }

    private String requestAccessToken(OAuthProvider provider, ProviderConfig config, String code, String state) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", config.clientId());
        params.put("redirect_uri", config.redirectUri());
        params.put("code", code);

        if (hasText(config.clientSecret())) {
            params.put("client_secret", config.clientSecret());
        }
        if (provider == OAuthProvider.NAVER) {
            params.put("state", state);
        }

        String endpoint = provider == OAuthProvider.KAKAO ? KAKAO_TOKEN_URL : NAVER_TOKEN_URL;
        JsonNode node = postForm(endpoint, toQueryString(params));
        String accessToken = textValue(node, "access_token");
        if (!hasText(accessToken)) {
            throw new UnauthorizedException(provider.displayName() + " 액세스 토큰 발급에 실패했습니다.");
        }
        return accessToken;
    }

    private SocialUserProfile requestSocialProfile(OAuthProvider provider, String accessToken) {
        JsonNode node = getWithBearer(provider == OAuthProvider.KAKAO ? KAKAO_ME_URL : NAVER_ME_URL, accessToken);
        if (provider == OAuthProvider.KAKAO) {
            String providerUserId = textValue(node, "id");
            String nickname = textValue(node.path("kakao_account").path("profile"), "nickname");
            if (!hasText(providerUserId)) {
                throw new UnauthorizedException("카카오 사용자 정보를 조회하지 못했습니다.");
            }
            return new SocialUserProfile(providerUserId, nickname);
        }

        JsonNode response = node.path("response");
        String providerUserId = textValue(response, "id");
        String nickname = textValue(response, "nickname");
        if (!hasText(providerUserId)) {
            throw new UnauthorizedException("네이버 사용자 정보를 조회하지 못했습니다.");
        }
        return new SocialUserProfile(providerUserId, nickname);
    }

    private JsonNode postForm(String url, String formBody) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(formBody))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UnauthorizedException("소셜 로그인 토큰 요청에 실패했습니다.");
            }
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new UnauthorizedException("소셜 로그인 토큰 요청 중 오류가 발생했습니다.");
        }
    }

    private JsonNode getWithBearer(String url, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UnauthorizedException("소셜 사용자 정보 조회에 실패했습니다.");
            }
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new UnauthorizedException("소셜 사용자 정보 조회 중 오류가 발생했습니다.");
        }
    }

    private String createState(OAuthProvider provider, String nextPath) {
        Map<String, Object> payload = Map.of(
            "provider", provider.code(),
            "next", nextPath,
            "iat", Instant.now().getEpochSecond(),
            "exp", Instant.now().plusSeconds(600).getEpochSecond(),
            "nonce", Long.toHexString(Double.doubleToLongBits(Math.random()))
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String payloadPart = encodeUrl(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signaturePart = encodeUrl(sign(payloadPart.getBytes(StandardCharsets.UTF_8)));
            return payloadPart + "." + signaturePart;
        } catch (Exception e) {
            throw new IllegalStateException("소셜 로그인 상태값 생성에 실패했습니다.");
        }
    }

    private OAuthState verifyState(OAuthProvider expectedProvider, String state) {
        String[] parts = state.split("\\.");
        if (parts.length != 2) {
            throw new BadRequestException("유효하지 않은 소셜 로그인 상태값입니다.");
        }

        byte[] expectedSignature = sign(parts[0].getBytes(StandardCharsets.UTF_8));
        byte[] actualSignature = decodeUrl(parts[1]);
        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw new BadRequestException("유효하지 않은 소셜 로그인 상태값입니다.");
        }

        try {
            JsonNode payload = objectMapper.readTree(decodeUrl(parts[0]));
            String providerText = textValue(payload, "provider");
            String next = textValue(payload, "next");
            long exp = payload.path("exp").asLong(-1L);
            long now = Instant.now().getEpochSecond();

            if (!Objects.equals(providerText, expectedProvider.code())) {
                throw new BadRequestException("유효하지 않은 소셜 로그인 상태값입니다.");
            }
            if (exp < now) {
                throw new BadRequestException("소셜 로그인 상태값이 만료되었습니다.");
            }
            return new OAuthState(normalizeNextPath(next));
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("유효하지 않은 소셜 로그인 상태값입니다.");
        }
    }

    private ProviderConfig getProviderConfig(OAuthProvider provider) {
        return provider == OAuthProvider.KAKAO ? kakaoConfig : naverConfig;
    }

    private void validateProviderConfig(OAuthProvider provider, ProviderConfig config) {
        if (!hasText(config.clientId()) || !hasText(config.redirectUri())) {
            throw new BadRequestException(provider.displayName() + " 로그인이 아직 설정되지 않았습니다.");
        }
    }

    private String generateUsername(OAuthProvider provider, String providerUserId) {
        String normalizedId = providerUserId == null ? "" : providerUserId.trim();
        if (normalizedId.isEmpty()) {
            throw new UnauthorizedException("소셜 사용자 식별값이 비어 있습니다.");
        }

        String sanitized = normalizedId.replaceAll("[^0-9A-Za-z_\\-]", "");
        if (sanitized.isEmpty()) {
            sanitized = Long.toHexString(normalizedId.hashCode() & 0xffffffffL);
        }
        if (sanitized.length() > 60) {
            sanitized = sanitized.substring(0, 60);
        }
        return provider.code() + "_" + sanitized;
    }

    public String buildLoginErrorRedirect(String message, String nextPath) {
        String safeNextPath = normalizeNextPath(nextPath);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("next", safeNextPath);
        params.put("oauthError", message);
        return frontendBaseUrl + "/login?" + toQueryString(params);
    }

    private String normalizeNextPath(String nextPath) {
        if (!hasText(nextPath)) {
            return "/webtoons";
        }
        String trimmed = nextPath.trim();
        if (!trimmed.startsWith("/")) {
            return "/webtoons";
        }
        return trimmed;
    }

    private String toQueryString(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!hasText(entry.getValue())) {
                continue;
            }
            if (!first) {
                builder.append("&");
            }
            first = false;
            builder.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }

        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "http://localhost:3000";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private byte[] sign(byte[] value) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(stateSecretBytes, "HmacSHA256"));
            return hmac.doFinal(value);
        } catch (Exception e) {
            throw new IllegalStateException("소셜 로그인 서명 처리 중 오류가 발생했습니다.");
        }
    }

    private String encodeUrl(byte[] value) {
        return URL_ENCODER.encodeToString(value);
    }

    private byte[] decodeUrl(String value) {
        try {
            return URL_DECODER.decode(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("유효하지 않은 인코딩 값입니다.");
        }
    }

    private enum OAuthProvider {
        KAKAO("kakao", "카카오"),
        NAVER("naver", "네이버");

        private final String code;
        private final String displayName;

        OAuthProvider(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String code() {
            return code;
        }

        public String displayName() {
            return displayName;
        }

        static OAuthProvider from(String value) {
            if ("kakao".equalsIgnoreCase(value)) {
                return KAKAO;
            }
            if ("naver".equalsIgnoreCase(value)) {
                return NAVER;
            }
            throw new BadRequestException("지원하지 않는 소셜 로그인 제공자입니다.");
        }
    }

    private record ProviderConfig(String clientId, String clientSecret, String redirectUri) {
    }

    private record SocialUserProfile(String providerUserId, String nickname) {
    }

    private record OAuthState(String nextPath) {
    }
}
