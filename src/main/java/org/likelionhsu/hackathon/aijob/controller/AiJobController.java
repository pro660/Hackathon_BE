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
import io.swagger.v3.oas.annotations.Parameter;
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
            description = """
                    PURCHASE_UTILITY, ITEM_ANALYSIS, STYLE_PLAN AI 작업을 생성합니다.
                    Idempotency-Key는 사용자 기준으로 멱등하게 처리하며 같은 Key에 다른 요청 본문을 사용하면 409가 반환됩니다.
                    사용자당 PENDING/PROCESSING AI 작업은 동시에 1개만 허용하고, 최근 24시간 기준 최대 10개의 AI 작업 생성을 허용합니다.
                    STYLE_PLAN context는 occasion, styleTags(1~4개), weatherCondition(nullable), prioritizeOwnedItems, language=ko를 사용합니다.
                    STYLE_PLAN은 동일 사용자·동일 입력·promptVersion·model의 최근 24시간 SUCCEEDED 결과가 있으면 OpenAI를 다시 호출하지 않고 재사용합니다.
                    새 작업은 202 Accepted, 완료된 멱등 재요청은 200 OK를 반환합니다.
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AiJobCreateResponse>>
            createAiJob(
                    @Parameter(
                            description = "사용자 기준 멱등성 키. 최대 255자",
                            required = true
                    )
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
            description = """
                    현재 로그인 사용자가 생성한 AI 작업을 조회합니다.
                    status는 PENDING, PROCESSING, SUCCEEDED, FAILED 중 하나입니다.
                    STYLE_PLAN 성공 시 result에 스마트 착용 추천 preview가 반환되고,
                    AI 생성이 최종 실패했지만 규칙 기반 추천이 가능한 경우 FAILED 상태와 함께 fallback이 반환됩니다.
                    """
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
