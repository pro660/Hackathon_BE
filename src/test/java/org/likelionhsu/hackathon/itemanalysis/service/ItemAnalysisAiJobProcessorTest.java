package org.likelionhsu.hackathon.itemanalysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetPurpose;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisAiJobGateway;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisException;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisGenerationResult;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisInputHasher;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisPort;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisRequest;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisResult;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ItemAnalysisAiJobProcessorTest {

    private static final Long USER_ID = 1L;
    private static final Long JOB_ID = 9101L;
    private static final Long IMAGE_ASSET_ID = 51L;

    @Mock
    private ItemAnalysisAiJobGateway aiJobGateway;

    @Mock
    private ImageAssetJdbcRepository imageAssetRepository;

    @Mock
    private ItemAnalysisInputHasher inputHasher;

    @Mock
    private ObjectProvider<ItemAnalysisPort> portProvider;

    @Mock
    private ItemAnalysisPort port;

    @Mock
    private ItemAnalysisAiJobCompletionService
            completionService;

    private ItemAnalysisAiJobProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ItemAnalysisAiJobProcessor(
                aiJobGateway,
                imageAssetRepository,
                inputHasher,
                portProvider,
                completionService
        );
    }

    @Test
    void alreadyClaimedJobIsNotProcessedTwice() {
        when(aiJobGateway.claimProcessing(
                USER_ID,
                JOB_ID
        )).thenReturn(false);

        ItemAnalysisAiJobProcessor.ProcessingResult result =
                processor.process(
                        USER_ID,
                        JOB_ID,
                        IMAGE_ASSET_ID
                );

        assertThat(result.outcome())
                .isEqualTo(
                        ItemAnalysisAiJobProcessor
                                .ProcessingOutcome
                                .NOT_CLAIMED
                );

        verifyNoInteractions(imageAssetRepository);
        verifyNoInteractions(inputHasher);
        verifyNoInteractions(portProvider);
        verifyNoInteractions(completionService);
    }

    @Test
    void boundTemporaryImageIsAnalyzedAndCompleted() {
        ImageAssetData asset = asset(JOB_ID);

        when(aiJobGateway.claimProcessing(
                USER_ID,
                JOB_ID
        )).thenReturn(true);

        when(imageAssetRepository.findOwnedItemAsset(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(Optional.of(asset));

        when(inputHasher.hash(asset))
                .thenReturn("a".repeat(64));

        when(aiJobGateway.updateInputHashIfProcessing(
                USER_ID,
                JOB_ID,
                "a".repeat(64)
        )).thenReturn(true);

        when(portProvider.getIfAvailable())
                .thenReturn(port);

        ItemAnalysisResult analysis =
                new ItemAnalysisResult(
                        "MCM",
                        "백팩",
                        ItemCategory.BAG,
                        ColorGroup.BLACK,
                        MaterialGroup.LEATHER
                );

        when(port.analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        )).thenReturn(
                new ItemAnalysisGenerationResult(
                        analysis,
                        100,
                        30,
                        700L
                )
        );

        ItemAnalysisAiJobProcessor.ProcessingResult result =
                processor.process(
                        USER_ID,
                        JOB_ID,
                        IMAGE_ASSET_ID
                );

        assertThat(result.outcome())
                .isEqualTo(
                        ItemAnalysisAiJobProcessor
                                .ProcessingOutcome
                                .SUCCEEDED
                );

        verify(completionService).completeSucceeded(
                USER_ID,
                JOB_ID,
                analysis,
                100,
                30,
                700L,
                0
        );
    }

    @Test
    void missingOrReboundImageFailsBeforeAiCall() {
        when(aiJobGateway.claimProcessing(
                USER_ID,
                JOB_ID
        )).thenReturn(true);

        when(imageAssetRepository.findOwnedItemAsset(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(
                Optional.of(
                        asset(9999L)
                )
        );

        ItemAnalysisAiJobProcessor.ProcessingResult result =
                processor.process(
                        USER_ID,
                        JOB_ID,
                        IMAGE_ASSET_ID
                );

        assertThat(result.outcome())
                .isEqualTo(
                        ItemAnalysisAiJobProcessor
                                .ProcessingOutcome.FAILED
                );

        verify(completionService).completeFailed(
                USER_ID,
                JOB_ID,
                null,
                0
        );

        verifyNoInteractions(inputHasher);
        verifyNoInteractions(portProvider);
        verifyNoInteractions(port);
    }

    @Test
    void unavailablePortFailsAfterInputHashIsStored() {
        ImageAssetData asset = asset(JOB_ID);

        when(aiJobGateway.claimProcessing(
                USER_ID,
                JOB_ID
        )).thenReturn(true);

        when(imageAssetRepository.findOwnedItemAsset(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(Optional.of(asset));

        when(inputHasher.hash(asset))
                .thenReturn("b".repeat(64));

        when(aiJobGateway.updateInputHashIfProcessing(
                USER_ID,
                JOB_ID,
                "b".repeat(64)
        )).thenReturn(true);

        when(portProvider.getIfAvailable())
                .thenReturn(null);

        ItemAnalysisAiJobProcessor.ProcessingResult result =
                processor.process(
                        USER_ID,
                        JOB_ID,
                        IMAGE_ASSET_ID
                );

        assertThat(result.outcome())
                .isEqualTo(
                        ItemAnalysisAiJobProcessor
                                .ProcessingOutcome.FAILED
                );

        verify(completionService).completeFailed(
                USER_ID,
                JOB_ID,
                null,
                0
        );

        verify(port, never()).analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        );
    }

    @Test
    void retryableFailureIsRetriedOnceAndCanSucceed() {
        prepareReadyAsset(
                "c".repeat(64)
        );

        ItemAnalysisResult analysis =
                analysisResult();

        when(port.analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        )).thenThrow(
                new ItemAnalysisException(
                        ItemAnalysisException
                                .FailureKind
                                .TRANSIENT_PROVIDER,
                        "temporary"
                )
        ).thenReturn(
                new ItemAnalysisGenerationResult(
                        analysis,
                        110,
                        35,
                        900L
                )
        );

        ItemAnalysisAiJobProcessor.ProcessingResult result =
                processor.process(
                        USER_ID,
                        JOB_ID,
                        IMAGE_ASSET_ID
                );

        assertThat(result.outcome())
                .isEqualTo(
                        ItemAnalysisAiJobProcessor
                                .ProcessingOutcome
                                .SUCCEEDED
                );

        verify(port, times(2)).analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        );

        verify(completionService).completeSucceeded(
                USER_ID,
                JOB_ID,
                analysis,
                110,
                35,
                900L,
                1
        );
    }

    @Test
    void retryableFailureThenSecondFailureStoresRetryCountOne() {
        prepareReadyAsset(
                "d".repeat(64)
        );

        when(port.analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        )).thenThrow(
                new ItemAnalysisException(
                        ItemAnalysisException
                                .FailureKind
                                .INVALID_RESPONSE,
                        "invalid response"
                )
        ).thenThrow(
                new IllegalStateException(
                        "second failure"
                )
        );

        ItemAnalysisAiJobProcessor.ProcessingResult result =
                processor.process(
                        USER_ID,
                        JOB_ID,
                        IMAGE_ASSET_ID
                );

        assertThat(result.outcome())
                .isEqualTo(
                        ItemAnalysisAiJobProcessor
                                .ProcessingOutcome.FAILED
                );

        verify(port, times(2)).analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        );

        verify(completionService).completeFailed(
                USER_ID,
                JOB_ID,
                null,
                1
        );
    }

    @Test
    void nonRetryableFailureIsNotRetried() {
        prepareReadyAsset(
                "e".repeat(64)
        );

        when(port.analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        )).thenThrow(
                new ItemAnalysisException(
                        ItemAnalysisException
                                .FailureKind
                                .NON_RETRYABLE_PROVIDER,
                        "refused"
                )
        );

        ItemAnalysisAiJobProcessor.ProcessingResult result =
                processor.process(
                        USER_ID,
                        JOB_ID,
                        IMAGE_ASSET_ID
                );

        assertThat(result.outcome())
                .isEqualTo(
                        ItemAnalysisAiJobProcessor
                                .ProcessingOutcome.FAILED
                );

        verify(port).analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        );

        verify(completionService).completeFailed(
                USER_ID,
                JOB_ID,
                null,
                0
        );
    }

    @Test
    void unexpectedRuntimeFailureIsNotRetried() {
        prepareReadyAsset(
                "f".repeat(64)
        );

        when(port.analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        )).thenThrow(
                new IllegalStateException(
                        "unexpected failure"
                )
        );

        ItemAnalysisAiJobProcessor.ProcessingResult result =
                processor.process(
                        USER_ID,
                        JOB_ID,
                        IMAGE_ASSET_ID
                );

        assertThat(result.outcome())
                .isEqualTo(
                        ItemAnalysisAiJobProcessor
                                .ProcessingOutcome.FAILED
                );

        verify(port).analyze(
                org.mockito.ArgumentMatchers
                        .any(ItemAnalysisRequest.class)
        );

        verify(completionService).completeFailed(
                USER_ID,
                JOB_ID,
                null,
                0
        );
    }

    private void prepareReadyAsset(
            String inputHash
    ) {
        ImageAssetData asset = asset(JOB_ID);

        when(aiJobGateway.claimProcessing(
                USER_ID,
                JOB_ID
        )).thenReturn(true);

        when(imageAssetRepository.findOwnedItemAsset(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(Optional.of(asset));

        when(inputHasher.hash(asset))
                .thenReturn(inputHash);

        when(aiJobGateway.updateInputHashIfProcessing(
                USER_ID,
                JOB_ID,
                inputHash
        )).thenReturn(true);

        when(portProvider.getIfAvailable())
                .thenReturn(port);
    }

    private ItemAnalysisResult analysisResult() {
        return new ItemAnalysisResult(
                "MCM",
                "백팩",
                ItemCategory.BAG,
                ColorGroup.BLACK,
                MaterialGroup.LEATHER
        );
    }

    private ImageAssetData asset(Long aiJobId) {
        return new ImageAssetData(
                IMAGE_ASSET_ID,
                USER_ID,
                ImageAssetPurpose.ITEM,
                null,
                aiJobId,
                "wear-it/user-items/test-image",
                "https://example.com/test-image.jpg",
                "jpg",
                2048L,
                1200,
                900,
                ImageAssetStatus.TEMPORARY,
                0,
                Instant.parse(
                        "2026-08-17T01:00:00Z"
                ),
                null,
                null
        );
    }
}
