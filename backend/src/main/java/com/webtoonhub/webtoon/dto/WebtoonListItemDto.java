package com.webtoonhub.webtoon.dto;

import java.util.List;

public record WebtoonListItemDto(
    Long id,
    String title,
    String author,
    String description,
    PlatformDto platform,
    List<CodeNameDto> genres,
    List<CodeNameDto> weekdays,
    String status,
    String statusName,
    String thumbnailUrl,
    String originalUrl
) {
}
