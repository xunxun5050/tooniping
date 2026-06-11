package com.webtoonhub.auth.controller;

import com.webtoonhub.auth.dto.AuthMeResponse;
import com.webtoonhub.auth.dto.EmailVerificationResponse;
import com.webtoonhub.auth.dto.EmailVerificationSendRequest;
import com.webtoonhub.auth.dto.EmailVerificationVerifyRequest;
import com.webtoonhub.auth.dto.LoginRequest;
import com.webtoonhub.auth.dto.LoginResponse;
import com.webtoonhub.auth.dto.SignupRequest;
import com.webtoonhub.auth.dto.UpdateNicknameRequest;
import com.webtoonhub.auth.dto.UserProfileDto;
import com.webtoonhub.auth.service.AuthService;
import com.webtoonhub.auth.service.EmailVerificationService;
import com.webtoonhub.auth.service.RefreshTokenService;
import com.webtoonhub.auth.service.SocialAuthService;
import com.webtoonhub.common.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "webtoon_hub_refresh_token";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
        AuthService authService,
        SocialAuthService socialAuthService,
        EmailVerificationService emailVerificationService
    ) {
        this.authService = authService;
        this.socialAuthService = socialAuthService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.login(request);
        attachRefreshCookie(servletRequest, response, result.refreshToken());
        return ApiResponse.ok(result.loginResponse());
    }

    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(
        @Valid @RequestBody SignupRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.signup(request);
        attachRefreshCookie(servletRequest, response, result.refreshToken());
        return ApiResponse.ok(result.loginResponse());
    }

    @PostMapping("/signup/email-code")
    public ApiResponse<EmailVerificationResponse> sendSignupEmailCode(
        @Valid @RequestBody EmailVerificationSendRequest request
    ) {
        return ApiResponse.ok(emailVerificationService.sendSignupCode(request.email()));
    }

    @PostMapping("/signup/email-code/verify")
    public ApiResponse<EmailVerificationResponse> verifySignupEmailCode(
        @Valid @RequestBody EmailVerificationVerifyRequest request
    ) {
        return ApiResponse.ok(emailVerificationService.verifySignupCode(request.email(), request.code()));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.refresh(readRefreshCookie(request));
        attachRefreshCookie(request, response, result.refreshToken());
        return ApiResponse.ok(result.loginResponse());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(readRefreshCookieOrNull(request));
        expireRefreshCookie(request, response);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> me(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String username = authService.authenticate(authorizationHeader);
        UserProfileDto profile = authService.getProfile(username);
        return ApiResponse.ok(new AuthMeResponse(
            profile.username(),
            profile.nickname(),
            profile.avatarSeed(),
            profile.avatarPalette()
        ));
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<UserProfileDto> updateNickname(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @Valid @RequestBody UpdateNicknameRequest request
    ) {
        String username = authService.authenticate(authorizationHeader);
        return ApiResponse.ok(authService.updateNickname(username, request.nickname()));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String username = authService.authenticate(authorizationHeader);
        authService.deleteAccount(username);
        expireRefreshCookie(request, response);
        return ApiResponse.ok(null);
    }

    @GetMapping("/oauth/{provider}/start")
    public void startOAuth(
        @PathVariable String provider,
        @RequestParam(required = false) String next,
        HttpServletResponse response
    ) throws IOException {
        String redirectUrl;
        try {
            redirectUrl = socialAuthService.buildAuthorizeUrl(provider, next);
        } catch (RuntimeException e) {
            redirectUrl = socialAuthService.buildLoginErrorRedirect(e.getMessage(), next);
        }
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/oauth/{provider}/callback")
    public void oauthCallback(
        @PathVariable String provider,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error,
        @RequestParam(required = false) String error_description,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        SocialAuthService.OAuthCallbackResult result;
        try {
            result = socialAuthService.handleCallback(provider, code, state, error, error_description);
        } catch (RuntimeException e) {
            result = new SocialAuthService.OAuthCallbackResult(
                socialAuthService.buildLoginErrorRedirect("소셜 로그인 콜백 처리에 실패했습니다.", "/webtoons"),
                null
            );
        }
        if (result.refreshToken() != null) {
            attachRefreshCookie(request, response, result.refreshToken());
        }
        response.sendRedirect(result.redirectUrl());
    }

    private String readRefreshCookie(HttpServletRequest request) {
        String refreshToken = readRefreshCookieOrNull(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.webtoonhub.common.exception.UnauthorizedException("로그인 유지 정보가 없습니다.");
        }
        return refreshToken;
    }

    private String readRefreshCookieOrNull(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void attachRefreshCookie(
        HttpServletRequest request,
        HttpServletResponse response,
        RefreshTokenService.RefreshToken refreshToken
    ) {
        Duration maxAge = Duration.between(Instant.now(), refreshToken.expiresAt());
        ResponseCookie cookie = baseRefreshCookie(request, refreshToken.value())
            .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void expireRefreshCookie(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = baseRefreshCookie(request, "")
            .maxAge(Duration.ZERO)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseRefreshCookie(HttpServletRequest request, String value) {
        boolean secure = isSecureRequest(request);
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite(secure ? "None" : "Lax")
            .path(REFRESH_TOKEN_COOKIE_PATH);
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedProtocol = request.getHeader("X-Forwarded-Protocol");
        String forwardedSsl = request.getHeader("X-Forwarded-Ssl");
        return request.isSecure()
            || "https".equalsIgnoreCase(forwardedProto)
            || "https".equalsIgnoreCase(forwardedProtocol)
            || "on".equalsIgnoreCase(forwardedSsl);
    }
}
