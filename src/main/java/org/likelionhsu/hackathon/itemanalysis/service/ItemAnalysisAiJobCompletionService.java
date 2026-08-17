package org.likelionhsu.hackathon.itemanalysis.service;

import java.util.Objects;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisAiJobGateway;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemAnalysisAiJobCompletionService {

    public static final String ANALYSIS_FAILED_ERROR_CODE =
            "AI_ITEM_ANALYSIS_FAILED";

    private final ItemAnalysisAiJobGateway aiJobGateway;
    private final ObjectMapper objectMapper;

    public ItemAnalysisAiJobCompletionService(
            ItemAnalysisAiJobGateway aiJobGateway,
            ObjectMapper objectMapper
    ) {
        this.aiJobGateway =
                Objects.requireNonNull(aiJobGateway);
        this.objectMapper =
                Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public void completeSucceeded(
            Long userId,
            Long jobId,
            ItemAnalysisResult result,
            Integer inputTokens,
            Integer outputTokens,
            Long latencyMs,
            int retryCount
    ) {
        Objects.requireNonNull(
                result,
                "result는 null일 수 없습니다."
        );

        boolean updated =
                aiJobGateway.markSucceeded(
                        userId,
                        jobId,
                        serialize(result),
                        inputTokens,
                        outputTokens,
                        latencyMs,
                        retryCount
                );

        requireUpdated(updated);
    }

    @Transactional
    public void completeFailed(
            Long userId,
            Long jobId,
            Long latencyMs,
            int retryCount
    ) {
        boolean updated =
                aiJobGateway.markFailed(
                        userId,
                        jobId,
                        ANALYSIS_FAILED_ERROR_CODE,
                        latencyMs,
                        retryCount
                );

        requireUpdated(updated);
    }

    private String serialize(
            ItemAnalysisResult result
    ) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "ITEM_ANALYSIS 결과 JSON 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    private void requireUpdated(
            boolean updated
    ) {
        if (!updated) {
            throw new IllegalStateException(
                    "ITEM_ANALYSIS AI Job 상태 전이에 실패했습니다."
            );
        }
    }
}
