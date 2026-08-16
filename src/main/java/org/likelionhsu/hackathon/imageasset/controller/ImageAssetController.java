package org.likelionhsu.hackathon.imageasset.controller;

import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.likelionhsu.hackathon.imageasset.dto.response.ImageAssetUploadResponse;
import org.likelionhsu.hackathon.imageasset.service.ImageAssetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;

@Tag(
        name = "Image Assets",
        description = "마이 아이템용 이미지 업로드 및 임시 이미지 관리 API"
)
@RestController
@RequestMapping("/api/image-assets")
public class ImageAssetController {

    private final ImageAssetService imageAssetService;

    public ImageAssetController(
            ImageAssetService imageAssetService
    ) {
        this.imageAssetService = imageAssetService;
    }

    @Operation(
            summary = "마이 아이템 이미지 업로드",
            description = "JPEG 또는 PNG 한 장을 업로드해 TEMPORARY ImageAsset을 생성합니다."
    )
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<
            ApiResponse<ImageAssetUploadResponse>
            > uploadImage(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ImageAssetUploadResponse response =
                imageAssetService
                        .uploadTemporaryItemImage(
                                Long.valueOf(
                                        jwt.getSubject()
                                ),
                                file
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(
            summary = "임시 이미지 삭제",
            description = "아직 마이 아이템에 연결되지 않은 TEMPORARY 이미지를 삭제합니다."
    )
    @DeleteMapping("/{imageAssetId}")
    public ResponseEntity<Void> deleteTemporaryImage(
            @PathVariable
            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            Long imageAssetId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        imageAssetService.deleteTemporaryItemImage(
                Long.valueOf(jwt.getSubject()),
                imageAssetId
        );

        return ResponseEntity.noContent().build();
    }
}
