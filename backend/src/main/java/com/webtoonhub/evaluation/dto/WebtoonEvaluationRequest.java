package com.webtoonhub.evaluation.dto;

import java.util.List;

public record WebtoonEvaluationRequest(
    String rating,
    List<String> emotionTags
) {
}
