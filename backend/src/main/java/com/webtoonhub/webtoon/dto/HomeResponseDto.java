package com.webtoonhub.webtoon.dto;

import java.util.List;

public record HomeResponseDto(
    List<SimpleWebtoonDto> recentWebtoons,
    List<MenuCountDto> weekdayMenus,
    List<MenuCountDto> genreMenus
) {
}
