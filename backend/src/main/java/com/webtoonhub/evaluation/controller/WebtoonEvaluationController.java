package com.webtoonhub.evaluation.controller;

import com.webtoonhub.auth.service.AuthService;
import com.webtoonhub.common.response.ApiResponse;
import com.webtoonhub.evaluation.dto.WebtoonEvaluationDto;
import com.webtoonhub.evaluation.dto.WebtoonEvaluationRequest;
import com.webtoonhub.evaluation.service.WebtoonEvaluationService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class WebtoonEvaluationController {

    private final WebtoonEvaluationService evaluationService;
    private final AuthService authService;

    public WebtoonEvaluationController(WebtoonEvaluationService evaluationService, AuthService authService) {
        this.evaluationService = evaluationService;
        this.authService = authService;
    }

    @GetMapping("/evaluations")
    public ApiResponse<List<WebtoonEvaluationDto>> getEvaluations(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String username = authService.authenticate(authorizationHeader);
        return ApiResponse.ok(evaluationService.getEvaluations(username));
    }

    @GetMapping("/evaluations/{webtoonId}")
    public ApiResponse<WebtoonEvaluationDto> getEvaluation(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable long webtoonId
    ) {
        String username = authService.authenticate(authorizationHeader);
        return ApiResponse.ok(evaluationService.getEvaluation(username, webtoonId));
    }

    @PutMapping("/evaluations/{webtoonId}")
    public ApiResponse<WebtoonEvaluationDto> saveEvaluation(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable long webtoonId,
        @RequestBody WebtoonEvaluationRequest request
    ) {
        String username = authService.authenticate(authorizationHeader);
        return ApiResponse.ok(evaluationService.saveEvaluation(username, webtoonId, request));
    }

    @DeleteMapping("/evaluations/{webtoonId}")
    public ApiResponse<Void> deleteEvaluation(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @PathVariable long webtoonId
    ) {
        String username = authService.authenticate(authorizationHeader);
        evaluationService.deleteEvaluation(username, webtoonId);
        return ApiResponse.ok(null);
    }
}
