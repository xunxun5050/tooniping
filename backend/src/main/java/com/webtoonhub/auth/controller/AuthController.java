package com.webtoonhub.auth.controller;

import com.webtoonhub.auth.dto.AuthMeResponse;
import com.webtoonhub.auth.dto.LoginRequest;
import com.webtoonhub.auth.dto.LoginResponse;
import com.webtoonhub.auth.dto.UpdateNicknameRequest;
import com.webtoonhub.auth.dto.UserProfileDto;
import com.webtoonhub.auth.service.AuthService;
import com.webtoonhub.auth.service.SocialAuthService;
import com.webtoonhub.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
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

    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    public AuthController(AuthService authService, SocialAuthService socialAuthService) {
        this.authService = authService;
        this.socialAuthService = socialAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> me(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String username = authService.authenticate(authorizationHeader);
        UserProfileDto profile = authService.getProfile(username);
        return ApiResponse.ok(new AuthMeResponse(profile.username(), profile.nickname()));
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<UserProfileDto> updateNickname(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @Valid @RequestBody UpdateNicknameRequest request
    ) {
        String username = authService.authenticate(authorizationHeader);
        return ApiResponse.ok(authService.updateNickname(username, request.nickname()));
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
        HttpServletResponse response
    ) throws IOException {
        String redirectUrl;
        try {
            redirectUrl = socialAuthService.handleCallback(provider, code, state, error, error_description);
        } catch (RuntimeException e) {
            redirectUrl = socialAuthService.buildLoginErrorRedirect("소셜 로그인 콜백 처리에 실패했습니다.", "/webtoons");
        }
        response.sendRedirect(redirectUrl);
    }
}
