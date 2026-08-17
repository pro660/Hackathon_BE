package org.likelionhsu.hackathon.place.controller;

import java.math.BigDecimal;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.dto.PlaceSearchResponse;
import org.likelionhsu.hackathon.place.service.PlaceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "Places",
        description = "Kakao Local 기반 장소 검색·추천·저장 API"
)
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @Operation(
            summary = "장소 검색",
            description = """
                    Kakao Local에서 실제 장소를 검색하고 places 캐시에 Upsert합니다.
                    query 또는 latitude/longitude + category 검색 조건이 필요합니다.
                    사용자 좌표 자체는 DB에 저장하지 않습니다.
                    최대 15개를 반환합니다.
                    """
    )
    @GetMapping
    public ApiResponse<PlaceSearchResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PlaceCategory category,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) Integer radius,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                placeService.search(
                        Long.valueOf(jwt.getSubject()),
                        query,
                        category,
                        latitude,
                        longitude,
                        radius
                )
        );
    }
}
