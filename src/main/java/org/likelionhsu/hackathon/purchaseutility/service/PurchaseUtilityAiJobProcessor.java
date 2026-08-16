package org.likelionhsu.hackathon.purchaseutility.service;

import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiJobGateway;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationPort;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationRequest;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationResult;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityAnalysisService.RuleAnalysisResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class PurchaseUtilityAiJobProcessor {

    private final PurchaseUtilityAiJobGateway aiJobGateway;
    private final PurchaseUtilityAnalysisService analysisService;
    private final ObjectProvider<PurchaseUtilityExplanationPort>
            explanationPortProvider;
    private final PurchaseUtilityAiJobCompletionService
            completionService;

    public PurchaseUtilityAiJobProcessor(
            PurchaseUtilityAiJobGateway aiJobGateway,
            PurchaseUtilityAnalysisService analysisService,
            ObjectProvider<PurchaseUtilityExplanationPort>
                    explanationPortProvider,
            PurchaseUtilityAiJobCompletionService
                    completionService
    ) {
        this.aiJobGateway = aiJobGateway;
        this.analysisService = analysisService;
        this.explanationPortProvider = explanationPortProvider;
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

        PurchaseUtilityExplanationPort explanationPort =
                explanationPortProvider.getIfAvailable();

        if (explanationPort == null) {
            completionService.completeReadyWithFallback(
                    userId,
                    jobId,
                    analysis.getId(),
                    analysis.getUtilityScore(),
                    "AI_GENERATION_FAILED",
                    0
            );

            return ProcessingResult.fallbackReady(
                    analysis.getId()
            );
        }

        PurchaseUtilityExplanationRequest request =
                PurchaseUtilityExplanationRequest.from(
                        analysis,
                        language
                );

        ExplanationAttempt attempt =
                generateWithSingleRetry(
                        explanationPort,
                        request
                );

        if (attempt.explanation() == null) {
            completionService.completeReadyWithFallback(
                    userId,
                    jobId,
                    analysis.getId(),
                    analysis.getUtilityScore(),
                    "AI_GENERATION_FAILED",
                    attempt.retryCount()
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
            PurchaseUtilityExplanationPort explanationPort,
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
