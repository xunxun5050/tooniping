package com.webtoonhub.auth.interceptor;

import com.webtoonhub.auth.service.AuthService;
import com.webtoonhub.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AdminAuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String username = authService.authenticate(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (!authService.isAdmin(username)) {
            throw new UnauthorizedException("관리자 권한이 필요합니다.");
        }
        request.setAttribute("authenticatedUsername", username);
        return true;
    }
}
