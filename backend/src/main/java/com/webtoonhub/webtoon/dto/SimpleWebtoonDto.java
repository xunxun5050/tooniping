package com.webtoonhub.webtoon.dto;

public record SimpleWebtoonDto(
    Long id,
    String title,
    String author,
    String thumbnailUrl
) {
}
