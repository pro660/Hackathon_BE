package org.likelionhsu.hackathon.place.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.place.dto.PlaceRecommendationRequest;
import org.likelionhsu.hackathon.place.dto.PlaceRecommendationResponse;
import org.likelionhsu.hackathon.place.service.PlaceRecommendationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Tag(
        name = "Places",
        description = "Kakao Local 기반 장소 검색·추천·저장 API"
)
@RestController
@RequestMapping("/api/style-plans/{stylePlanId}/place-recommendations")
public class StylePlanPlaceRecommendationController {

    private final PlaceRecommendationService recommendationService;

    public StylePlanPlaceRecommendationController(
            PlaceRecommendationService recommendationService
    ) {
        this.recommendationService = recommendationService;
    }

    @Operation(
            summary = "스마트 착용 추천 기반 장소 추천",
            description = """
                    현재 사용자가 소유한 StylePlan의 occasion을 사용합니다.
                    Kakao Local 후보를 서버 Rule-Based로 순위화하며 OpenAI를 호출하지 않습니다.
                    category 적합도 최대 60점 + 거리 적합도 최대 40점입니다.
                    최대 3개를 반환하고 기존 style_plan_places를 교체합니다.
                    """
    )
    @PostMapping
    public ApiResponse<PlaceRecommendationResponse> recommend(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long stylePlanId,
            @Valid @RequestBody
            PlaceRecommendationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                recommendationService.recommend(
                        Long.valueOf(jwt.getSubject()),
                        stylePlanId,
                        request
                )
        );
    }
}
