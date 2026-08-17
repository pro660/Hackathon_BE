package org.likelionhsu.hackathon.styleplan.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationResult;
import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanAiJobGateway;
import org.springframework.stereotype.Service;

@Service
public class StylePlanAiJobCompletionService {

    private final StylePlanAiJobGateway aiJobGateway;
    private final ObjectMapper objectMapper;

    public StylePlanAiJobCompletionService(
            StylePlanAiJobGateway aiJobGateway,
            ObjectMapper objectMapper
    ) {
        this.aiJobGateway = aiJobGateway;
        this.objectMapper = objectMapper;
    }

    public void completeSuccess(
            Long userId,
            Long jobId,
            StylePlanPreview preview,
            StylePlanGenerationResult generation,
            int retryCount
    ) {
        boolean updated = aiJobGateway.markSucceeded(
                userId,
                jobId,
                serialize(preview),
                generation.inputTokens(),
                generation.outputTokens(),
                generation.latencyMs(),
                retryCount
        );

        requireUpdated(updated);
    }

    public void completeCached(
            Long userId,
            Long jobId,
            String cachedResultJson
    ) {
        boolean updated = aiJobGateway.markSucceeded(
                userId,
                jobId,
                cachedResultJson,
                null,
                null,
                null,
                0
        );

        requireUpdated(updated);
    }

    public void completeFallback(
            Long userId,
            Long jobId,
            StylePlanPreview fallback,
            String errorCode,
            int retryCount
    ) {
        boolean updated =
                aiJobGateway.markFailedWithFallback(
                        userId,
                        jobId,
                        serialize(fallback),
                        errorCode,
                        retryCount,
                        null
                );

        requireUpdated(updated);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(
                    value
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "STYLE_PLAN 결과 JSON 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    private void requireUpdated(boolean updated) {
        if (!updated) {
            throw new IllegalStateException(
                    "STYLE_PLAN AI Job 상태 전이에 실패했습니다."
            );
        }
    }
}
