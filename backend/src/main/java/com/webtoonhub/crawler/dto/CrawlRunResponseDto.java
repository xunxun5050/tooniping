package com.webtoonhub.crawler.dto;

import java.time.LocalDateTime;

public record CrawlRunResponseDto(
    Long crawlHistoryId,
    String crawlType,
    String status,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    String message
) {
}
