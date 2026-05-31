package com.webtoonhub.webtoon.dto;

public record WebtoonSearchCondition(
    String keyword,
    String platform,
    String genre,
    String weekday,
    String status,
    int page,
    int size,
    String sort
) {
}
