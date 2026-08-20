package org.likelionhsu.hackathon.user.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.user.dto.request.UserNotificationSettingsUpdateRequest;
import org.likelionhsu.hackathon.user.dto.response.UserNotificationSettingsResponse;
import org.likelionhsu.hackathon.user.service.UserNotificationSettingsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(
        name = "Users",
        description = "현재 로그인 사용자의 정보 관리 API"
)
@RestController
@RequestMapping("/api/users/me/notification-settings")
public class UserNotificationSettingsController {

    private final UserNotificationSettingsService
            settingsService;

    public UserNotificationSettingsController(
            UserNotificationSettingsService settingsService
    ) {
        this.settingsService = settingsService;
    }

    @Operation(
            summary = "알림·마케팅 설정 조회",
            description = "관리 일정, 추천 업데이트, 마케팅 PUSH, 이메일 수신 설정을 조회합니다."
    )
    @GetMapping
    public ApiResponse<UserNotificationSettingsResponse>
    getSettings(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                settingsService.getSettings(
                        Long.valueOf(jwt.getSubject())
                )
        );
    }

    @Operation(
            summary = "알림·마케팅 설정 저장",
            description = "관리 일정, 추천 업데이트, 마케팅 PUSH, 이메일 수신 설정을 한 번에 저장합니다."
    )
    @PatchMapping
    public ApiResponse<UserNotificationSettingsResponse>
    updateSettings(
            @Valid @RequestBody
            UserNotificationSettingsUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                settingsService.updateSettings(
                        Long.valueOf(jwt.getSubject()),
                        request
                )
        );
    }
}
