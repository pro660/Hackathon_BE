package org.likelionhsu.hackathon.careguide.controller;

import org.likelionhsu.hackathon.careguide.dto.CareReminderSettingRequest;
import org.likelionhsu.hackathon.careguide.dto.CareReminderSettingResponse;
import org.likelionhsu.hackathon.careguide.service.CareReminderSettingService;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Tag(
        name = "My Item Care Reminder",
        description = "마이 아이템 관리 알림 설정 API"
)
@RestController
@RequestMapping("/api/my-items/{myItemId}/care-reminder-setting")
public class CareReminderSettingController {

    private final CareReminderSettingService reminderSettingService;

    public CareReminderSettingController(
            CareReminderSettingService reminderSettingService
    ) {
        this.reminderSettingService = reminderSettingService;
    }

    @Operation(
            summary = "관리 알림 설정 조회",
            description = "마이 아이템별 관리 알림 ON/OFF 상태를 조회합니다."
    )
    @GetMapping
    public ApiResponse<CareReminderSettingResponse> getSetting(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long myItemId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                reminderSettingService.getSetting(
                        Long.valueOf(jwt.getSubject()),
                        myItemId
                )
        );
    }

    @Operation(
            summary = "관리 알림 설정 변경",
            description = "마이 아이템별 관리 알림을 멱등적으로 켜거나 끕니다."
    )
    @PutMapping
    public ApiResponse<CareReminderSettingResponse> updateSetting(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long myItemId,
            @Valid @RequestBody
            CareReminderSettingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                reminderSettingService.updateSetting(
                        Long.valueOf(jwt.getSubject()),
                        myItemId,
                        request
                )
        );
    }
}
