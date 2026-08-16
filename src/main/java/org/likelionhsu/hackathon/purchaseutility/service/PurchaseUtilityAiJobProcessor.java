package org.likelionhsu.hackathon.purchaseutility.service;

import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiJobGateway;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationPort;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationRequest;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationResult;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityAnalysisService.RuleAnalysisResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(PurchaseUtilityExplanationPort.class)
public class PurchaseUtilityAiJobProcessor {

    private final PurchaseUtilityAiJobGateway aiJobGateway;
    private final PurchaseUtilityAnalysisService analysisService;
    private final PurchaseUtilityExplanationPort explanationPort;
    private final PurchaseUtilityAiJobCompletionService
            completionService;

    public PurchaseUtilityAiJobProcessor(
            PurchaseUtilityAiJobGateway aiJobGateway,
            PurchaseUtilityAnalysisService analysisService,
            PurchaseUtilityExplanationPort explanationPort,
            PurchaseUtilityAiJobCompletionService
                    completionService
    ) {
        this.aiJobGateway = aiJobGateway;
        this.analysisService = analysisService;
        this.explanationPort = explanationPort;
        this.completionService = completionService;
    }

    public ProcessingResult process(
            Long userId,
            Long jobId,
            Long productId,
            String language
    ) {
        if (!aiJobGateway.claimProcessing(
                userId,
                jobId
        )) {
            return ProcessingResult.notClaimed();
        }

        RuleAnalysisResult ruleResult =
                analysisService.createRuleBasedAnalysis(
                        userId,
                        productId,
                        jobId
                );

        if (!ruleResult.isReady()) {
            completionService.completeInsufficient(
                    userId,
                    jobId,
                    ruleResult.message()
            );

            return ProcessingResult.insufficient();
        }

        PurchaseUtilityAnalysis analysis =
                ruleResult.analysis();

        PurchaseUtilityExplanationRequest request =
                PurchaseUtilityExplanationRequest.from(
                        analysis,
                        language
                );

        ExplanationAttempt attempt =
                generateWithSingleRetry(request);

        if (attempt.explanation() == null) {
            completionService.completeReadyWithFallback(
                    userId,
                    jobId,
                    analysis.getId(),
                    analysis.getUtilityScore(),
                    null
            );

            return ProcessingResult.fallbackReady(
                    analysis.getId()
            );
        }

        completionService.completeReadyWithAi(
                userId,
                jobId,
                analysis.getId(),
                attempt.explanation(),
                attempt.retryCount()
        );

        return ProcessingResult.aiReady(
                analysis.getId()
        );
    }

    private ExplanationAttempt generateWithSingleRetry(
            PurchaseUtilityExplanationRequest request
    ) {
        try {
            return new ExplanationAttempt(
                    explanationPort.generate(request),
                    0
            );
        } catch (RuntimeException firstFailure) {
            try {
                return new ExplanationAttempt(
                        explanationPort.generate(request),
                        1
                );
            } catch (RuntimeException secondFailure) {
                return new ExplanationAttempt(
                        null,
                        1
                );
            }
        }
    }

    private record ExplanationAttempt(
            PurchaseUtilityExplanationResult explanation,
            int retryCount
    ) {
    }

    public enum ProcessingOutcome {
        NOT_CLAIMED,
        INSUFFICIENT_DATA,
        READY_AI,
        READY_RULE_BASED_FALLBACK
    }

    public record ProcessingResult(
            ProcessingOutcome outcome,
            Long analysisId
    ) {

        public static ProcessingResult notClaimed() {
            return new ProcessingResult(
                    ProcessingOutcome.NOT_CLAIMED,
                    null
            );
        }

        public static ProcessingResult insufficient() {
            return new ProcessingResult(
                    ProcessingOutcome.INSUFFICIENT_DATA,
                    null
            );
        }

        public static ProcessingResult aiReady(
                Long analysisId
        ) {
            return new ProcessingResult(
                    ProcessingOutcome.READY_AI,
                    analysisId
            );
        }

        public static ProcessingResult fallbackReady(
                Long analysisId
        ) {
            return new ProcessingResult(
                    ProcessingOutcome
                            .READY_RULE_BASED_FALLBACK,
                    analysisId
            );
        }
    }
}
