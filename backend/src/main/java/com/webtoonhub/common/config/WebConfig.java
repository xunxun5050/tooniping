package com.webtoonhub.common.config;

import com.webtoonhub.auth.interceptor.AdminAuthInterceptor;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final String[] allowedOrigins;

    public WebConfig(
        AdminAuthInterceptor adminAuthInterceptor,
        @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}") String allowedOrigins
    ) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.allowedOrigins = parseAllowedOrigins(allowedOrigins);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }

    private static String[] parseAllowedOrigins(String origins) {
        Set<String> parsedOrigins = new LinkedHashSet<>();
        Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .forEach(parsedOrigins::add);
        return parsedOrigins.toArray(String[]::new);
    }
}
