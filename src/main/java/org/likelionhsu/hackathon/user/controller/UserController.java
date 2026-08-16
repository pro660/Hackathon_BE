package org.likelionhsu.hackathon.user.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.user.dto.request.UserProfileUpdateRequest;
import org.likelionhsu.hackathon.user.dto.response.UserProfileResponse;
import org.likelionhsu.hackathon.user.service.UserService;
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
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인 사용자의 닉네임과 성별을 조회합니다."
    )
    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                userService.getMyProfile(
                        Long.valueOf(jwt.getSubject())
                )
        );
    }

    @Operation(
            summary = "내 정보 수정",
            description = "현재 로그인 사용자의 닉네임 또는 성별을 부분 수정합니다."
    )
    @PatchMapping
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                userService.updateMyProfile(
                        Long.valueOf(jwt.getSubject()),
                        request
                )
        );
    }
}
