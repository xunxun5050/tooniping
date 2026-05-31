package com.webtoonhub.webtoon.dto;

import java.util.List;

public record WebtoonFiltersDto(
    List<CodeNameDto> platforms,
    List<CodeNameDto> genres,
    List<CodeNameDto> weekdays,
    List<CodeNameDto> statuses
) {
}
