package com.webtoonhub.auth.dto;

public record EmailVerificationResponse(
    String email,
    String expiresAt,
    boolean verified
) {
}
