package org.likelionhsu.hackathon.aijob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.itemanalysis.service.ItemAnalysisAiJobDispatcher;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityAiJobDispatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiJobItemAnalysisServiceTest {

    private static final Long USER_ID = 1L;
    private static final String IDEMPOTENCY_KEY =
            "item-analysis-key";

    @Mock
    private AiJobJdbcRepository repository;

    @Mock
    private ItemAnalysisAiJobCreationService creationService;

    @Mock
    private ItemAnalysisAiJobDispatcher
            itemAnalysisDispatcher;

    @Mock
    private PurchaseUtilityAiJobDispatcher
            purchaseUtilityDispatcher;

    private AiJobRequestHasher hasher;
    private AiJobService service;

    @BeforeEach
    void setUp() {
        hasher = new AiJobRequestHasher();
        service = new AiJobService(
                repository,
                hasher,
                new ObjectMapper(),
                creationService,
                itemAnalysisDispatcher,
                purchaseUtilityDispatcher,
                "test-model"
        );
    }

    @Test
    void newItemAnalysisJobIsCreatedAndAccepted() {
        String requestHash =
                hasher.hashItemAnalysis("51");

        when(repository.findByUserAndIdempotencyKey(
                USER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(Optional.empty());

        when(creationService.createPendingAndBind(
                USER_ID,
                51L,
                IDEMPOTENCY_KEY,
                requestHash,
                "test-model",
                "item-analysis-v1"
        )).thenReturn(
                itemJob(
                        9101L,
                        AiJobStatus.PENDING,
                        requestHash
                )
        );

        AiJobService.CreationResult result =
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request("00051")
                );

        assertThat(result.accepted()).isTrue();
        assertThat(result.response().jobId())
                .isEqualTo("9101");
        assertThat(result.response().type())
                .isEqualTo(AiJobType.ITEM_ANALYSIS);
        assertThat(result.response().status())
                .isEqualTo(AiJobStatus.PENDING);

        verify(creationService).createPendingAndBind(
                USER_ID,
                51L,
                IDEMPOTENCY_KEY,
                requestHash,
                "test-model",
                "item-analysis-v1"
        );
        verify(itemAnalysisDispatcher).dispatch(
                USER_ID,
                9101L,
                51L
        );
        verifyNoInteractions(purchaseUtilityDispatcher);
    }

    @Test
    void sameItemAnalysisRequestReplaysExistingJob() {
        String requestHash =
                hasher.hashItemAnalysis("51");

        when(repository.findByUserAndIdempotencyKey(
                USER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(
                Optional.of(
                        itemJob(
                                9101L,
                                AiJobStatus.SUCCEEDED,
                                requestHash
                        )
                )
        );

        AiJobService.CreationResult result =
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request("51")
                );

        assertThat(result.accepted()).isFalse();
        assertThat(result.response().jobId())
                .isEqualTo("9101");

        verifyNoInteractions(creationService);
        verifyNoInteractions(itemAnalysisDispatcher);
        verifyNoInteractions(purchaseUtilityDispatcher);
    }

    @Test
    void invalidImageAssetIdIsRejectedBeforeCreation() {
        assertThatThrownBy(() ->
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request("0")
                )
        ).isInstanceOf(
                RequestValidationException.class
        );

        verifyNoInteractions(repository);
        verifyNoInteractions(creationService);
        verifyNoInteractions(itemAnalysisDispatcher);
        verifyNoInteractions(purchaseUtilityDispatcher);
    }

    private AiJobCreateRequest request(
            String imageAssetId
    ) {
        return new AiJobCreateRequest(
                AiJobType.ITEM_ANALYSIS,
                new AiJobCreateRequest.Context(
                        null,
                        imageAssetId
                )
        );
    }

    private AiJobData itemJob(
            Long jobId,
            AiJobStatus status,
            String requestHash
    ) {
        Instant createdAt =
                Instant.parse(
                        "2026-08-17T01:00:00Z"
                );

        Instant startedAt =
                status == AiJobStatus.PROCESSING
                        ? createdAt
                        : null;

        Instant completedAt =
                status == AiJobStatus.SUCCEEDED
                        || status == AiJobStatus.FAILED
                        ? createdAt.plusSeconds(10)
                        : null;

        return new AiJobData(
                jobId,
                USER_ID,
                AiJobType.ITEM_ANALYSIS,
                status,
                IDEMPOTENCY_KEY,
                requestHash,
                "test-model",
                "item-analysis-v1",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                startedAt,
                completedAt,
                createdAt,
                createdAt
        );
    }
}
