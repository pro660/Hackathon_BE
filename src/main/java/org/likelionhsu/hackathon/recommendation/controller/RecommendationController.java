package org.likelionhsu.hackathon.recommendation.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.recommendation.dto.request.RecommendationRequest;
import org.likelionhsu.hackathon.recommendation.dto.response.RecommendationResponse;
import org.likelionhsu.hackathon.recommendation.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(
        name = "Recommendations",
        description = "현재 로그인 사용자의 MCM 제품 추천 API"
)
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(
            RecommendationService recommendationService
    ) {
        this.recommendationService = recommendationService;
    }

    @Operation(
            summary = "MCM 제품 추천 생성",
            description = "취향과 현재 상황을 기준으로 Rule-Based MCM 제품 추천을 생성합니다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecommendationResponse> createRecommendation(
            @Valid @RequestBody RecommendationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                recommendationService.createRecommendation(
                        Long.valueOf(jwt.getSubject()),
                        request
                )
        );
    }

    @Operation(
            summary = "MCM 제품 추천 상세 조회",
            description = "현재 로그인 사용자가 생성한 추천 결과를 조회합니다."
    )
    @GetMapping("/{recommendationId}")
    public ApiResponse<RecommendationResponse> getRecommendation(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                recommendationService.getRecommendation(
                        Long.valueOf(jwt.getSubject()),
                        recommendationId
                )
        );
    }
}
