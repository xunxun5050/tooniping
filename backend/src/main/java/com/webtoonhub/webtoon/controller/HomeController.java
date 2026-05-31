package com.webtoonhub.webtoon.controller;

import com.webtoonhub.common.response.ApiResponse;
import com.webtoonhub.webtoon.dto.HomeResponseDto;
import com.webtoonhub.webtoon.service.WebtoonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final WebtoonService webtoonService;

    public HomeController(WebtoonService webtoonService) {
        this.webtoonService = webtoonService;
    }

    @GetMapping("/home")
    public ApiResponse<HomeResponseDto> getHomeData() {
        return ApiResponse.ok(webtoonService.getHomeData());
    }
}
