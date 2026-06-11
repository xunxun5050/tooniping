package com.webtoonhub.favorite.dto;

import com.webtoonhub.webtoon.dto.CodeNameDto;
import com.webtoonhub.webtoon.dto.PlatformDto;
import java.util.List;

public record FavoriteWebtoonDto(
    Long id,
    String title,
    String author,
    String thumbnailUrl,
    PlatformDto platform,
    String status,
    String statusName,
    String originalUrl,
    List<CodeNameDto> weekdays,
    String addedAt
) {
}
