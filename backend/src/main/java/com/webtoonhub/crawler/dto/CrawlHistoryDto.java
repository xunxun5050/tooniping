package com.webtoonhub.crawler.dto;

import java.time.LocalDateTime;

public record CrawlHistoryDto(
    Long id,
    String platformCode,
    String crawlType,
    String status,
    int totalCount,
    int successCount,
    int failCount,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    String message,
    LocalDateTime createdAt
) {
}
