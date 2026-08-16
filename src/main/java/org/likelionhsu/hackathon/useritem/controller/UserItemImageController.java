package org.likelionhsu.hackathon.useritem.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemImageLinkResponse;
import org.likelionhsu.hackathon.useritem.service.UserItemImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

@Tag(
        name = "My Item Images",
        description = "마이 아이템 이미지 연결·교체·삭제 API"
)
@RestController
@RequestMapping("/api/my-items")
public class UserItemImageController {

    private final UserItemImageService userItemImageService;

    public UserItemImageController(
            UserItemImageService userItemImageService
    ) {
        this.userItemImageService =
                userItemImageService;
    }

    @Operation(
            summary = "마이 아이템 이미지 연결 또는 교체",
            description = "TEMPORARY 이미지를 마이 아이템에 연결하고 기존 ACTIVE 이미지가 있으면 교체합니다."
    )
    @PutMapping(
            "/{myItemId}/images/{imageAssetId}"
    )
    public ApiResponse<UserItemImageLinkResponse>
    attachImage(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long myItemId,
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long imageAssetId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                userItemImageService.attach(
                        Long.valueOf(jwt.getSubject()),
                        myItemId,
                        imageAssetId
                )
        );
    }

    @Operation(
            summary = "마이 아이템 연결 이미지 삭제",
            description = "ACTIVE 연결 이미지를 삭제 대기로 전환하고 저장소 삭제를 시도합니다."
    )
    @DeleteMapping(
            "/{myItemId}/images/{imageAssetId}"
    )
    public ResponseEntity<Void> deleteImage(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long myItemId,
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long imageAssetId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        userItemImageService.delete(
                Long.valueOf(jwt.getSubject()),
                myItemId,
                imageAssetId
        );

        return ResponseEntity.noContent().build();
    }
}
