package org.likelionhsu.hackathon.careguide.controller;

import org.likelionhsu.hackathon.careguide.dto.CareCalendarResponse;
import org.likelionhsu.hackathon.careguide.dto.CareGuideResponse;
import org.likelionhsu.hackathon.careguide.dto.StorageGuideResponse;
import org.likelionhsu.hackathon.careguide.service.CareGuideService;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

@Tag(
        name = "My Item Care",
        description = "마이 아이템 소재 기반 맞춤 관리 가이드 API"
)
@RestController
@RequestMapping("/api/my-items/{myItemId}")
public class CareGuideController {

    private final CareGuideService careGuideService;

    public CareGuideController(
            CareGuideService careGuideService
    ) {
        this.careGuideService = careGuideService;
    }

    @Operation(
            summary = "맞춤 관리 가이드 조회",
            description = "마이 아이템의 확정 소재를 기준으로 관리 방법과 권장 관리 주기를 조회합니다."
    )
    @GetMapping("/care-guide")
    public ApiResponse<CareGuideResponse> getCareGuide(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long myItemId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                careGuideService.getCareGuide(
                        Long.valueOf(jwt.getSubject()),
                        myItemId
                )
        );
    }

    @Operation(
            summary = "관리 캘린더 조회",
            description = "구매일과 소재별 권장 주기를 기준으로 지정한 월의 관리 일정을 계산합니다."
    )
    @GetMapping("/care-calendar")
    public ApiResponse<CareCalendarResponse> getCareCalendar(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long myItemId,
            @RequestParam String month,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                careGuideService.getCareCalendar(
                        Long.valueOf(jwt.getSubject()),
                        myItemId,
                        month
                )
        );
    }

    @Operation(
            summary = "보관법 조회",
            description = "소재별 피해야 할 환경, 습기 관리, 추천 보관법을 조회합니다."
    )
    @GetMapping("/storage-guide")
    public ApiResponse<StorageGuideResponse> getStorageGuide(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long myItemId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                careGuideService.getStorageGuide(
                        Long.valueOf(jwt.getSubject()),
                        myItemId
                )
        );
    }
}
