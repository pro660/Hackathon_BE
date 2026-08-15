package org.likelionhsu.hackathon.preference.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.preference.dto.request.PreferenceRequest;
import org.likelionhsu.hackathon.preference.dto.response.PreferenceResponse;
import org.likelionhsu.hackathon.preference.service.PreferenceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(
        name = "Preferences",
        description = "현재 로그인 사용자의 취향 프로필 API"
)
@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(
            PreferenceService preferenceService
    ) {
        this.preferenceService = preferenceService;
    }

    @Operation(
            summary = "취향 프로필 조회",
            description = "현재 로그인 사용자의 취향 프로필을 조회합니다."
    )
    @GetMapping
    public ApiResponse<PreferenceResponse> getPreference(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                preferenceService.getPreference(
                        Long.valueOf(jwt.getSubject())
                )
        );
    }

    @Operation(
            summary = "취향 프로필 저장",
            description = "현재 로그인 사용자의 취향 프로필을 전체 교체 방식으로 저장합니다."
    )
    @PutMapping
    public ApiResponse<PreferenceResponse> updatePreference(
            @Valid @RequestBody PreferenceRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                preferenceService.updatePreference(
                        Long.valueOf(jwt.getSubject()),
                        request
                )
        );
    }
}