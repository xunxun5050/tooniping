package com.webtoonhub.webtoon.controller;

import com.webtoonhub.common.response.ApiResponse;
import com.webtoonhub.webtoon.dto.WebtoonFiltersDto;
import com.webtoonhub.webtoon.service.WebtoonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WebtoonFilterController {

    private final WebtoonService webtoonService;

    public WebtoonFilterController(WebtoonService webtoonService) {
        this.webtoonService = webtoonService;
    }

    @GetMapping("/webtoon-filters")
    public ApiResponse<WebtoonFiltersDto> getFilters() {
        return ApiResponse.ok(webtoonService.getFilters());
    }
}
