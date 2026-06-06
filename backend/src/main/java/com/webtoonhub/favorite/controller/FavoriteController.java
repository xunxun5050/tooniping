package com.webtoonhub.favorite.controller;

import com.webtoonhub.auth.service.AuthService;
import com.webtoonhub.common.response.ApiResponse;
import com.webtoonhub.favorite.dto.FavoriteWebtoonDto;
import com.webtoonhub.favorite.service.FavoriteService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final AuthService authService;

    public FavoriteController(FavoriteService favoriteService, AuthService authService) {
        this.favoriteService = favoriteService;
        this.authService = authService;
    }

    @GetMapping("/favorites")
    public ApiResponse<List<FavoriteWebtoonDto>> getFavorites(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String username = authService.authenticate(authorizationHeader);
        return ApiResponse.ok(favoriteService.getFavorites(username));
    }

    @PutMapping("/favorites/{webtoonId}")
    public ApiResponse<FavoriteWebtoonDto> addFavorite(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable long webtoonId
    ) {
        String username = authService.authenticate(authorizationHeader);
        return ApiResponse.ok(favoriteService.addFavorite(username, webtoonId));
    }

    @DeleteMapping("/favorites/{webtoonId}")
    public ApiResponse<Void> removeFavorite(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable long webtoonId
    ) {
        String username = authService.authenticate(authorizationHeader);
        favoriteService.removeFavorite(username, webtoonId);
        return ApiResponse.ok(null);
    }
}
