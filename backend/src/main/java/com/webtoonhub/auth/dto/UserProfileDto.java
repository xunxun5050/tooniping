package com.webtoonhub.auth.dto;

import java.time.LocalDateTime;

public record UserProfileDto(
    String username,
    String nickname,
    String provider,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
