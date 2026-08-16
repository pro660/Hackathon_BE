package org.likelionhsu.hackathon.aijob.controller;

import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;
import org.likelionhsu.hackathon.aijob.dto.response.AiJobCreateResponse;
import org.likelionhsu.hackathon.aijob.dto.response.AiJobResponse;
import org.likelionhsu.hackathon.aijob.service.AiJobService;
import org.likelionhsu.hackathon.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Tag(
        name = "AI Jobs",
        description = "AI 비동기 작업 생성 및 조회 API"
)
@RestController
@RequestMapping("/api/ai-jobs")
public class AiJobController {

    private final AiJobService aiJobService;

    public AiJobController(
            AiJobService aiJobService
    ) {
        this.aiJobService = aiJobService;
    }

    @Operation(
            summary = "AI 작업 생성",
            description = "구매 전 활용 가능성 AI 작업을 멱등하게 생성합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AiJobCreateResponse>>
            createAiJob(
                    @RequestHeader(
                            value = "Idempotency-Key",
                            required = false
                    )
                    @NotBlank(message = "필수 입력값입니다.")
                    @Size(
                            max = 255,
                            message = "255자 이하로 입력해 주세요."
                    )
                    String idempotencyKey,
                    @Valid @RequestBody
                    AiJobCreateRequest request,
                    @AuthenticationPrincipal
                    Jwt jwt
            ) {

        AiJobService.CreationResult result =
                aiJobService.create(
                        Long.valueOf(jwt.getSubject()),
                        idempotencyKey,
                        request
                );

        HttpStatus status =
                result.accepted()
                        ? HttpStatus.ACCEPTED
                        : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.success(
                                result.response()
                        )
                );
    }

    @Operation(
            summary = "AI 작업 조회",
            description = "현재 로그인 사용자가 생성한 AI 작업 상태와 결과를 조회합니다."
    )
    @GetMapping("/{jobId}")
    public ApiResponse<AiJobResponse> getAiJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                aiJobService.get(
                        Long.valueOf(jwt.getSubject()),
                        jobId
                )
        );
    }
}
