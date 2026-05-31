package com.webtoonhub.webtoon.controller;

import com.webtoonhub.common.response.ApiResponse;
import com.webtoonhub.webtoon.dto.PagedResultDto;
import com.webtoonhub.webtoon.dto.WebtoonDetailDto;
import com.webtoonhub.webtoon.dto.WebtoonListItemDto;
import com.webtoonhub.webtoon.dto.WebtoonSearchCondition;
import com.webtoonhub.webtoon.service.WebtoonService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WebtoonController {

    private final WebtoonService webtoonService;

    public WebtoonController(WebtoonService webtoonService) {
        this.webtoonService = webtoonService;
    }

    @GetMapping("/webtoons")
    public ApiResponse<PagedResultDto<WebtoonListItemDto>> getWebtoons(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String platform,
        @RequestParam(required = false) String genre,
        @RequestParam(required = false) String weekday,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "latest") String sort
    ) {
        WebtoonSearchCondition condition = new WebtoonSearchCondition(
            keyword,
            platform,
            genre,
            weekday,
            status,
            page,
            size,
            sort
        );
        return ApiResponse.ok(webtoonService.getWebtoons(condition));
    }

    @GetMapping("/webtoons/{webtoonId}")
    public ApiResponse<WebtoonDetailDto> getWebtoon(@PathVariable long webtoonId) {
        return ApiResponse.ok(webtoonService.getWebtoonDetail(webtoonId));
    }

    @GetMapping("/webtoons/{webtoonId}/similar")
    public ApiResponse<List<WebtoonListItemDto>> getSimilarWebtoons(
        @PathVariable long webtoonId,
        @RequestParam(defaultValue = "6") int size
    ) {
        return ApiResponse.ok(webtoonService.getSimilarWebtoons(webtoonId, size));
    }
}
