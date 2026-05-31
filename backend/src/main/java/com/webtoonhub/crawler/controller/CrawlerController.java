package com.webtoonhub.crawler.controller;

import com.webtoonhub.common.response.ApiResponse;
import com.webtoonhub.crawler.dto.CrawlHistoryDto;
import com.webtoonhub.crawler.dto.CrawlRunResponseDto;
import com.webtoonhub.crawler.service.CrawlerService;
import com.webtoonhub.webtoon.dto.PagedResultDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/crawlers/naver-webtoon/initial")
    public ApiResponse<CrawlRunResponseDto> runInitialCrawler() {
        return ApiResponse.ok(crawlerService.runInitialCrawler());
    }

    @PostMapping("/crawlers/naver-webtoon/weekly")
    public ApiResponse<CrawlRunResponseDto> runWeeklyCrawler() {
        return ApiResponse.ok(crawlerService.runWeeklyCrawler());
    }

    @GetMapping("/crawl-histories")
    public ApiResponse<PagedResultDto<CrawlHistoryDto>> getCrawlHistories(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(crawlerService.getCrawlHistories(page, size));
    }

    @GetMapping("/crawl-histories/{crawlHistoryId}")
    public ApiResponse<CrawlHistoryDto> getCrawlHistory(@PathVariable long crawlHistoryId) {
        return ApiResponse.ok(crawlerService.getCrawlHistory(crawlHistoryId));
    }
}
