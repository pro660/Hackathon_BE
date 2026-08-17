package org.likelionhsu.hackathon.styleplan.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.likelionhsu.hackathon.styleplan.repository.StylePlanAiJobGateway;
import org.springframework.stereotype.Service;

@Service
public class StylePlanAiJobProcessor {

    public static final String STYLE_PLAN_FAILED_ERROR_CODE =
            "AI_STYLE_PLAN_FAILED";

    private final StylePlanAiJobGateway aiJobGateway;
    private final StylePlanFallbackService fallbackService;
    private final ObjectMapper objectMapper;

    public StylePlanAiJobProcessor(
            StylePlanAiJobGateway aiJobGateway,
            StylePlanFallbackService fallbackService,
            ObjectMapper objectMapper
    ) {
        this.aiJobGateway = aiJobGateway;
        this.fallbackService = fallbackService;
        this.objectMapper = objectMapper;
    }

    public ProcessingResult process(
            Long userId,
            Long jobId,
            StylePlanJobRequest request
    ) {
        if (!aiJobGateway.claimProcessing(
                userId,
                jobId
        )) {
            return ProcessingResult.notClaimed();
        }

        StylePlanFallbackService.BuildResult fallback =
                fallbackService.build(
                        userId,
                        jobId,
                        request
                );

        if (!aiJobGateway.updateInputHashIfProcessing(
                userId,
                jobId,
                fallback.inputHash()
        )) {
            throw new IllegalStateException(
                    "STYLE_PLAN input_hash 저장에 실패했습니다."
            );
        }

        boolean updated =
                aiJobGateway.markFailedWithFallback(
                        userId,
                        jobId,
                        serialize(fallback.preview()),
                        STYLE_PLAN_FAILED_ERROR_CODE
                );

        if (!updated) {
            throw new IllegalStateException(
                    "STYLE_PLAN fallback 상태 저장에 실패했습니다."
            );
        }

        return ProcessingResult.fallback();
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "STYLE_PLAN fallback JSON 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    public enum ProcessingOutcome {
        NOT_CLAIMED,
        FALLBACK
    }

    public record ProcessingResult(
            ProcessingOutcome outcome
    ) {

        public static ProcessingResult notClaimed() {
            return new ProcessingResult(
                    ProcessingOutcome.NOT_CLAIMED
            );
        }

        public static ProcessingResult fallback() {
            return new ProcessingResult(
                    ProcessingOutcome.FALLBACK
            );
        }
    }
}
