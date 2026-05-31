package com.webtoonhub.admin.controller;

import com.webtoonhub.admin.dto.AdminWebtoonUpsertRequest;
import com.webtoonhub.admin.service.AdminWebtoonService;
import com.webtoonhub.common.response.ApiResponse;
import com.webtoonhub.webtoon.dto.PagedResultDto;
import com.webtoonhub.webtoon.dto.WebtoonDetailDto;
import com.webtoonhub.webtoon.dto.WebtoonListItemDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/webtoons")
public class AdminWebtoonController {

    private final AdminWebtoonService adminWebtoonService;

    public AdminWebtoonController(AdminWebtoonService adminWebtoonService) {
        this.adminWebtoonService = adminWebtoonService;
    }

    @GetMapping
    public ApiResponse<PagedResultDto<WebtoonListItemDto>> getWebtoons(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminWebtoonService.getWebtoons(keyword, page, size));
    }

    @GetMapping("/{webtoonId}")
    public ApiResponse<WebtoonDetailDto> getWebtoon(@PathVariable long webtoonId) {
        return ApiResponse.ok(adminWebtoonService.getWebtoon(webtoonId));
    }

    @PostMapping
    public ApiResponse<WebtoonDetailDto> createWebtoon(
        @Valid @RequestBody AdminWebtoonUpsertRequest request
    ) {
        return ApiResponse.ok(adminWebtoonService.createWebtoon(request));
    }

    @PutMapping("/{webtoonId}")
    public ApiResponse<WebtoonDetailDto> updateWebtoon(
        @PathVariable long webtoonId,
        @Valid @RequestBody AdminWebtoonUpsertRequest request
    ) {
        return ApiResponse.ok(adminWebtoonService.updateWebtoon(webtoonId, request));
    }

    @PatchMapping("/{webtoonId}/inactive")
    public ApiResponse<WebtoonDetailDto> deactivateWebtoon(@PathVariable long webtoonId) {
        return ApiResponse.ok(adminWebtoonService.setActive(webtoonId, false));
    }

    @PatchMapping("/{webtoonId}/active")
    public ApiResponse<WebtoonDetailDto> activateWebtoon(@PathVariable long webtoonId) {
        return ApiResponse.ok(adminWebtoonService.setActive(webtoonId, true));
    }

    @PostMapping("/{webtoonId}/thumbnail/refresh")
    public ApiResponse<WebtoonDetailDto> refreshThumbnail(@PathVariable long webtoonId) {
        return ApiResponse.ok(adminWebtoonService.refreshThumbnail(webtoonId));
    }
}
