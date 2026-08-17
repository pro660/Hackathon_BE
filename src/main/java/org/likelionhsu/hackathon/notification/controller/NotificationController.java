package org.likelionhsu.hackathon.notification.controller;

import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.notification.dto.NotificationReadUpdateRequest;
import org.likelionhsu.hackathon.notification.dto.NotificationResponse;
import org.likelionhsu.hackathon.notification.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Tag(
        name = "Notifications",
        description = "현재 로그인 사용자의 서비스 내부 알림 API"
)
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "알림 목록 조회",
            description = "현재 로그인 사용자의 서비스 내부 알림을 페이지 단위로 조회합니다."
    )
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>>
    getNotifications(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "0 이상이어야 합니다.")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "1 이상이어야 합니다.")
            @Max(value = 100, message = "100 이하여야 합니다.")
            int size,
            @RequestParam(defaultValue = "createdAt,desc")
            String sort,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                notificationService.getNotifications(
                        Long.valueOf(jwt.getSubject()),
                        page,
                        size,
                        parseAscending(sort)
                )
        );
    }

    @Operation(
            summary = "알림 읽음 상태 변경",
            description = "본인 알림의 읽음/안읽음 상태를 변경합니다."
    )
    @PatchMapping("/{notificationId}")
    public ApiResponse<NotificationResponse> updateRead(
            @PathVariable
            @Min(value = 1, message = "1 이상이어야 합니다.")
            Long notificationId,
            @Valid @RequestBody
            NotificationReadUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                notificationService.updateRead(
                        Long.valueOf(jwt.getSubject()),
                        notificationId,
                        request
                )
        );
    }

    private boolean parseAscending(String sort) {
        if ("createdAt,asc".equals(sort)) {
            return true;
        }

        if ("createdAt,desc".equals(sort)) {
            return false;
        }

        throw new RequestValidationException(
                "sort",
                "createdAt,asc 또는 createdAt,desc만 지원합니다."
        );
    }
}
