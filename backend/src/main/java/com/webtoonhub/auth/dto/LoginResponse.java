package com.webtoonhub.auth.dto;

public record LoginResponse(
    String token,
    String tokenType,
    String username,
    String nickname,
    String avatarSeed,
    String avatarPalette,
    String expiresAt,
    String loginWeekday
) {
}
