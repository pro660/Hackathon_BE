package org.likelionhsu.hackathon.purchaseutility.service;

import java.math.BigDecimal;

import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiJobGateway;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseUtilityAiJobCompletionService {

    private final PurchaseUtilityAiJobGateway aiJobGateway;
    private final PurchaseUtilityAnalysisFinalizationService
            finalizationService;

    public PurchaseUtilityAiJobCompletionService(
            PurchaseUtilityAiJobGateway aiJobGateway,
            PurchaseUtilityAnalysisFinalizationService
                    finalizationService
    ) {
        this.aiJobGateway = aiJobGateway;
        this.finalizationService = finalizationService;
    }

    @Transactional
    public void completeReadyWithAi(
            Long userId,
            Long jobId,
            Long analysisId,
            PurchaseUtilityExplanationResult explanation,
            int retryCount
    ) {
        finalizationService.applyAiExplanation(
                userId,
                analysisId,
                explanation.summary()
        );

        boolean updated =
                aiJobGateway.markSucceeded(
                        userId,
                        jobId,
                        readyResultJson(analysisId),
                        explanation.inputTokens(),
                        explanation.outputTokens(),
                        explanation.latencyMs(),
                        retryCount
                );

        requireUpdated(updated);
    }

    @Transactional
    public void completeInsufficient(
            Long userId,
            Long jobId,
            String message
    ) {
        boolean updated =
                aiJobGateway.markSucceeded(
                        userId,
                        jobId,
                        insufficientResultJson(message),
                        null,
                        null,
                        null,
                        0
                );

        requireUpdated(updated);
    }

    @Transactional
    public void completeReadyWithFallback(
            Long userId,
            Long jobId,
            Long analysisId,
            BigDecimal utilityScore,
            String errorCode,
            int retryCount
    ) {
        boolean updated =
                aiJobGateway.markFailed(
                        userId,
                        jobId,
                        fallbackJson(
                                analysisId,
                                utilityScore
                        ),
                        errorCode,
                        retryCount,
                        null
                );

        requireUpdated(updated);
    }

    private String readyResultJson(
            Long analysisId
    ) {
        return """
                {"status":"READY","analysisId":"%d"}
                """.formatted(analysisId).trim();
    }

    private String insufficientResultJson(
            String message
    ) {
        return """
                {
                  "status":"INSUFFICIENT_DATA",
                  "analysisId":null,
                  "message":"%s"
                }
                """
                .formatted(escapeJson(message))
                .strip();
    }

    private String fallbackJson(
            Long analysisId,
            BigDecimal utilityScore
    ) {
        return """
                {
                  "type":"RULE_BASED",
                  "result":{
                    "status":"READY",
                    "analysisId":"%d",
                    "utilityScore":%s
                  }
                }
                """
                .formatted(
                        analysisId,
                        utilityScore.toPlainString()
                )
                .strip();
    }

    private String escapeJson(
            String value
    ) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private void requireUpdated(
            boolean updated
    ) {
        if (!updated) {
            throw new IllegalStateException(
                    "AI Job 상태 전이에 실패했습니다."
            );
        }
    }
}
