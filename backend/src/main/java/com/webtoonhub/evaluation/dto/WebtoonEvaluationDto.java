package com.webtoonhub.evaluation.dto;

import java.util.List;

public record WebtoonEvaluationDto(
    Long webtoonId,
    String title,
    String author,
    String thumbnailUrl,
    String rating,
    List<String> emotionTags,
    String createdAt,
    String updatedAt
) {
}
