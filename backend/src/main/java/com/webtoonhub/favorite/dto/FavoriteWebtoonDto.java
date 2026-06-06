package com.webtoonhub.favorite.dto;

import com.webtoonhub.webtoon.dto.CodeNameDto;
import java.util.List;

public record FavoriteWebtoonDto(
    Long id,
    String title,
    String author,
    String thumbnailUrl,
    String statusName,
    String originalUrl,
    List<CodeNameDto> weekdays,
    String addedAt
) {
}
