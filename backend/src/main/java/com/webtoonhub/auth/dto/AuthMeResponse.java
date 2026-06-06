package com.webtoonhub.auth.dto;

public record AuthMeResponse(
    String username,
    String nickname
) {
}
