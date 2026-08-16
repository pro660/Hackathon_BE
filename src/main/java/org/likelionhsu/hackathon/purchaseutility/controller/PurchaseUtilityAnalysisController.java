package org.likelionhsu.hackathon.purchaseutility.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.purchaseutility.dto.response.PurchaseUtilityAnalysisResponse;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityAnalysisQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

@Tag(
        name = "Purchase Utility Analyses",
        description = "현재 로그인 사용자의 구매 전 활용 가능성 분석 조회 API"
)
@RestController
@RequestMapping("/api/purchase-utility-analyses")
public class PurchaseUtilityAnalysisController {

    private final PurchaseUtilityAnalysisQueryService queryService;

    public PurchaseUtilityAnalysisController(
            PurchaseUtilityAnalysisQueryService queryService
    ) {
        this.queryService = queryService;
    }

    @Operation(
            summary = "구매 전 활용 가능성 분석 상세 조회",
            description = "현재 로그인 사용자가 생성한 구매 전 활용 가능성 분석 결과를 조회합니다."
    )
    @GetMapping("/{analysisId}")
    public ApiResponse<PurchaseUtilityAnalysisResponse> getAnalysis(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long analysisId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                queryService.getAnalysis(
                        Long.valueOf(jwt.getSubject()),
                        analysisId
                )
        );
    }
}
