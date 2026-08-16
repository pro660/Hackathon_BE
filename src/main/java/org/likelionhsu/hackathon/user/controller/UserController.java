package org.likelionhsu.hackathon.user.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.user.dto.request.UserProfileUpdateRequest;
import org.likelionhsu.hackathon.user.dto.response.UserProfileResponse;
import org.likelionhsu.hackathon.user.service.UserService;
import org.likelionhsu.hackathon.user.service.AccountDeletionService;
import org.likelionhsu.hackathon.auth.support.ReauthenticationCookieService;
import org.likelionhsu.hackathon.auth.support.RefreshCookieService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@Tag(
        name = "Users",
        description = "현재 로그인 사용자의 정보 관리 API"
)
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;
    private final AccountDeletionService accountDeletionService;
    private final ReauthenticationCookieService
            reauthenticationCookieService;
    private final RefreshCookieService refreshCookieService;

    public UserController(
            UserService userService,
            AccountDeletionService accountDeletionService,
            ReauthenticationCookieService
                    reauthenticationCookieService,
            RefreshCookieService refreshCookieService
    ) {
        this.userService = userService;
        this.accountDeletionService = accountDeletionService;
        this.reauthenticationCookieService =
                reauthenticationCookieService;
        this.refreshCookieService = refreshCookieService;
    }

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인 사용자의 닉네임, 성별, 재인증 가능한 로그인 방식을 조회합니다."
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

    @Operation(
            summary = "회원 탈퇴",
            description = "10분 이내에 완료한 재인증을 확인한 뒤 계정과 사용자 소유 데이터를 삭제합니다."
    )
    @DeleteMapping
    public ResponseEntity<Void> deleteMyAccount(
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        accountDeletionService.deleteAccount(
                Long.valueOf(jwt.getSubject()),
                reauthenticationCookieService.read(request)
        );

        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        reauthenticationCookieService
                                .clear()
                                .toString(),
                        refreshCookieService.clear().toString()
                )
                .build();
    }
}
