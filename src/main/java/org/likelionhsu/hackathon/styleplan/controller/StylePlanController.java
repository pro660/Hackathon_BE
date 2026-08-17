package org.likelionhsu.hackathon.styleplan.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanCreateRequest;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanCreateResponse;
import org.likelionhsu.hackathon.styleplan.service.StylePlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(
        name = "Smart Wear Recommendations",
        description = "스마트 착용 추천 결과 저장 및 관리 API"
)
@RestController
@RequestMapping("/api/style-plans")
public class StylePlanController {

    private final StylePlanService stylePlanService;

    public StylePlanController(
            StylePlanService stylePlanService
    ) {
        this.stylePlanService = stylePlanService;
    }

    @Operation(
            summary = "스마트 착용 추천 저장",
            description = """
                    STYLE_PLAN 미리보기를 사용자가 확정한 뒤 저장합니다.
                    AI Job 성공만으로 자동 저장하지 않습니다.
                    aiJobId가 있으면 현재 사용자의 STYLE_PLAN 결과와 아이템/상품 조합을 재검증합니다.
                    AI 성공 결과는 AI, 규칙 기반 fallback은 RULE_BASED, aiJobId가 없는 직접 저장은 MANUAL로 기록합니다.
                    보유 아이템은 최대 10개, MCM 추천 상품은 최대 3개입니다.
                    """
    )
    @PostMapping
    public ResponseEntity<
            ApiResponse<StylePlanCreateResponse>>
            create(
                    @Valid @RequestBody
                    StylePlanCreateRequest request,
                    @AuthenticationPrincipal
                    Jwt jwt
            ) {
        StylePlanCreateResponse response =
                stylePlanService.create(
                        Long.valueOf(jwt.getSubject()),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(response)
                );
    }
}
