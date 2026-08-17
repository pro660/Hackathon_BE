package org.likelionhsu.hackathon.itemanalysis.service;

import java.util.Objects;

import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisAiJobGateway;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisException;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisGenerationResult;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisInputHasher;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisPort;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ItemAnalysisAiJobProcessor {

    private final ItemAnalysisAiJobGateway aiJobGateway;
    private final ImageAssetJdbcRepository imageAssetRepository;
    private final ItemAnalysisInputHasher inputHasher;
    private final ObjectProvider<ItemAnalysisPort>
            itemAnalysisPortProvider;
    private final ItemAnalysisAiJobCompletionService
            completionService;

    public ItemAnalysisAiJobProcessor(
            ItemAnalysisAiJobGateway aiJobGateway,
            ImageAssetJdbcRepository imageAssetRepository,
            ItemAnalysisInputHasher inputHasher,
            ObjectProvider<ItemAnalysisPort>
                    itemAnalysisPortProvider,
            ItemAnalysisAiJobCompletionService
                    completionService
    ) {
        this.aiJobGateway =
                Objects.requireNonNull(aiJobGateway);
        this.imageAssetRepository =
                Objects.requireNonNull(
                        imageAssetRepository
                );
        this.inputHasher =
                Objects.requireNonNull(inputHasher);
        this.itemAnalysisPortProvider =
                Objects.requireNonNull(
                        itemAnalysisPortProvider
                );
        this.completionService =
                Objects.requireNonNull(
                        completionService
                );
    }

    public ProcessingResult process(
            Long userId,
            Long jobId,
            Long imageAssetId
    ) {
        if (!aiJobGateway.claimProcessing(
                userId,
                jobId
        )) {
            return ProcessingResult.notClaimed();
        }

        ImageAssetData asset =
                findBoundTemporaryAsset(
                        userId,
                        jobId,
                        imageAssetId
                );

        if (asset == null) {
            completionService.completeFailed(
                    userId,
                    jobId,
                    null,
                    0
            );

            return ProcessingResult.failed();
        }

        String inputHash =
                inputHasher.hash(asset);

        boolean inputStored =
                aiJobGateway
                        .updateInputHashIfProcessing(
                                userId,
                                jobId,
                                inputHash
                        );

        if (!inputStored) {
            throw new IllegalStateException(
                    "ITEM_ANALYSIS input_hash 저장에 실패했습니다."
            );
        }

        ItemAnalysisPort port =
                itemAnalysisPortProvider
                        .getIfAvailable();

        if (port == null) {
            completionService.completeFailed(
                    userId,
                    jobId,
                    null,
                    0
            );

            return ProcessingResult.failed();
        }

        AnalysisAttempt attempt =
                analyzeWithSingleRetry(
                        port,
                        ItemAnalysisRequest.from(asset)
                );

        if (attempt.generated() == null) {
            completionService.completeFailed(
                    userId,
                    jobId,
                    null,
                    attempt.retryCount()
            );

            return ProcessingResult.failed();
        }

        ItemAnalysisGenerationResult generated =
                attempt.generated();

        completionService.completeSucceeded(
                userId,
                jobId,
                generated.result(),
                generated.inputTokens(),
                generated.outputTokens(),
                generated.latencyMs(),
                attempt.retryCount()
        );

        return ProcessingResult.succeeded();
    }

    private AnalysisAttempt analyzeWithSingleRetry(
            ItemAnalysisPort port,
            ItemAnalysisRequest request
    ) {
        try {
            return new AnalysisAttempt(
                    port.analyze(request),
                    0
            );
        } catch (ItemAnalysisException firstFailure) {
            if (!firstFailure.isRetryable()) {
                return new AnalysisAttempt(
                        null,
                        0
                );
            }

            try {
                return new AnalysisAttempt(
                        port.analyze(request),
                        1
                );
            } catch (RuntimeException secondFailure) {
                return new AnalysisAttempt(
                        null,
                        1
                );
            }
        } catch (RuntimeException unexpectedFailure) {
            return new AnalysisAttempt(
                    null,
                    0
            );
        }
    }

    private record AnalysisAttempt(
            ItemAnalysisGenerationResult generated,
            int retryCount
    ) {
    }

    private ImageAssetData findBoundTemporaryAsset(
            Long userId,
            Long jobId,
            Long imageAssetId
    ) {
        return imageAssetRepository
                .findOwnedItemAsset(
                        userId,
                        imageAssetId
                )
                .filter(asset ->
                        asset.status()
                                == ImageAssetStatus.TEMPORARY
                                && asset.userItemId() == null
                                && asset.deletedAt() == null
                                && Objects.equals(
                                        asset.aiJobId(),
                                        jobId
                                )
                )
                .orElse(null);
    }

    public enum ProcessingOutcome {
        NOT_CLAIMED,
        SUCCEEDED,
        FAILED
    }

    public record ProcessingResult(
            ProcessingOutcome outcome
    ) {

        public static ProcessingResult notClaimed() {
            return new ProcessingResult(
                    ProcessingOutcome.NOT_CLAIMED
            );
        }

        public static ProcessingResult succeeded() {
            return new ProcessingResult(
                    ProcessingOutcome.SUCCEEDED
            );
        }

        public static ProcessingResult failed() {
            return new ProcessingResult(
                    ProcessingOutcome.FAILED
            );
        }
    }
}
