package com.webtoonhub.webtoon.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WebtoonDetailDto(
    Long id,
    String title,
    String author,
    String description,
    PlatformDto platform,
    List<CodeNameDto> genres,
    List<CodeNameDto> weekdays,
    String status,
    String statusName,
    ThumbnailDto thumbnail,
    String originalUrl,
    LocalDateTime lastCrawledAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
