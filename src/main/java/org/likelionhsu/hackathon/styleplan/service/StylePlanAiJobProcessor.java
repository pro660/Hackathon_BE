package org.likelionhsu.hackathon.styleplan.service;

import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationException;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationPort;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationResult;
import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanAiJobGateway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class StylePlanAiJobProcessor {

    public static final String STYLE_PLAN_FAILED_ERROR_CODE =
            "AI_STYLE_PLAN_FAILED";

    private final StylePlanAiJobGateway aiJobGateway;
    private final StylePlanRecommendationContextService
            contextService;
    private final StylePlanInputHasher inputHasher;
    private final StylePlanFallbackService fallbackService;
    private final ObjectProvider<StylePlanGenerationPort>
            generationPortProvider;
    private final StylePlanPreviewAssembler previewAssembler;
    private final StylePlanAiJobCompletionService
            completionService;

    public StylePlanAiJobProcessor(
            StylePlanAiJobGateway aiJobGateway,
            StylePlanRecommendationContextService
                    contextService,
            StylePlanInputHasher inputHasher,
            StylePlanFallbackService fallbackService,
            ObjectProvider<StylePlanGenerationPort>
                    generationPortProvider,
            StylePlanPreviewAssembler previewAssembler,
            StylePlanAiJobCompletionService
                    completionService
    ) {
        this.aiJobGateway = aiJobGateway;
        this.contextService = contextService;
        this.inputHasher = inputHasher;
        this.fallbackService = fallbackService;
        this.generationPortProvider =
                generationPortProvider;
        this.previewAssembler = previewAssembler;
        this.completionService = completionService;
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

        StylePlanRecommendationContext context =
                contextService.prepare(
                        userId,
                        request
                );

        String inputHash =
                inputHasher.hash(context);

        if (!aiJobGateway.updateInputHashIfProcessing(
                userId,
                jobId,
                inputHash
        )) {
            throw new IllegalStateException(
                    "STYLE_PLAN input_hash 저장에 실패했습니다."
            );
        }

        StylePlanPreview fallback =
                fallbackService.build(
                        jobId,
                        context
                );

        StylePlanGenerationPort generationPort =
                generationPortProvider.getIfAvailable();

        if (generationPort == null) {
            completionService.completeFallback(
                    userId,
                    jobId,
                    fallback,
                    STYLE_PLAN_FAILED_ERROR_CODE,
                    0
            );

            return ProcessingResult.fallback();
        }

        GenerationAttempt attempt =
                generateWithSingleRetry(
                        generationPort,
                        context,
                        jobId
                );

        if (attempt.preview() == null) {
            completionService.completeFallback(
                    userId,
                    jobId,
                    fallback,
                    STYLE_PLAN_FAILED_ERROR_CODE,
                    attempt.retryCount()
            );

            return ProcessingResult.fallback();
        }

        completionService.completeSuccess(
                userId,
                jobId,
                attempt.preview(),
                attempt.generation(),
                attempt.retryCount()
        );

        return ProcessingResult.ai();
    }

    private GenerationAttempt generateWithSingleRetry(
            StylePlanGenerationPort generationPort,
            StylePlanRecommendationContext context,
            Long jobId
    ) {
        try {
            return successfulAttempt(
                    generationPort,
                    context,
                    jobId,
                    0
            );
        } catch (StylePlanGenerationException
                firstFailure) {
            if (!firstFailure.isRetryable()) {
                return GenerationAttempt.failed(0);
            }

            try {
                return successfulAttempt(
                        generationPort,
                        context,
                        jobId,
                        1
                );
            } catch (RuntimeException secondFailure) {
                return GenerationAttempt.failed(1);
            }
        } catch (RuntimeException unexpectedFailure) {
            return GenerationAttempt.failed(0);
        }
    }

    private GenerationAttempt successfulAttempt(
            StylePlanGenerationPort generationPort,
            StylePlanRecommendationContext context,
            Long jobId,
            int retryCount
    ) {
        StylePlanGenerationResult generation =
                generationPort.generate(context);

        StylePlanPreview preview =
                previewAssembler.assemble(
                        jobId,
                        context,
                        generation.selection()
                );

        return GenerationAttempt.success(
                preview,
                generation,
                retryCount
        );
    }

    private record GenerationAttempt(
            StylePlanPreview preview,
            StylePlanGenerationResult generation,
            int retryCount
    ) {

        private static GenerationAttempt success(
                StylePlanPreview preview,
                StylePlanGenerationResult generation,
                int retryCount
        ) {
            return new GenerationAttempt(
                    preview,
                    generation,
                    retryCount
            );
        }

        private static GenerationAttempt failed(
                int retryCount
        ) {
            return new GenerationAttempt(
                    null,
                    null,
                    retryCount
            );
        }
    }

    public enum ProcessingOutcome {
        NOT_CLAIMED,
        AI,
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

        public static ProcessingResult ai() {
            return new ProcessingResult(
                    ProcessingOutcome.AI
            );
        }

        public static ProcessingResult fallback() {
            return new ProcessingResult(
                    ProcessingOutcome.FALLBACK
            );
        }
    }
}
