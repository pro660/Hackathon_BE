package org.likelionhsu.hackathon.aijob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class AiJobServiceTest {

    private static final Long USER_ID = 1L;
    private static final String IDEMPOTENCY_KEY =
            "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private AiJobJdbcRepository repository;

    private AiJobRequestHasher hasher;
    private AiJobService service;

    @BeforeEach
    void setUp() {
        hasher = new AiJobRequestHasher();
        service = new AiJobService(
                repository,
                hasher,
                "test-model"
        );
    }

    @Test
    void newPurchaseUtilityJobIsPendingAndAccepted() {
        String requestHash =
                hasher.hashPurchaseUtility("123");

        when(repository.findByUserAndIdempotencyKey(
                USER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(Optional.empty());

        when(repository.createPending(
                USER_ID,
                AiJobType.PURCHASE_UTILITY,
                IDEMPOTENCY_KEY,
                requestHash,
                "test-model",
                "purchase-utility-summary-v1"
        )).thenReturn(9001L);

        when(repository.findOwned(
                USER_ID,
                9001L
        )).thenReturn(
                Optional.of(
                        job(
                                9001L,
                                AiJobStatus.PENDING,
                                requestHash
                        )
                )
        );

        AiJobService.CreationResult result =
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request("123")
                );

        assertThat(result.accepted()).isTrue();
        assertThat(result.response().jobId())
                .isEqualTo("9001");
        assertThat(result.response().status())
                .isEqualTo(AiJobStatus.PENDING);
    }

    @Test
    void samePendingRequestReplaysExistingJob() {
        String requestHash =
                hasher.hashPurchaseUtility("123");

        when(repository.findByUserAndIdempotencyKey(
                USER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(
                Optional.of(
                        job(
                                9001L,
                                AiJobStatus.PENDING,
                                requestHash
                        )
                )
        );

        AiJobService.CreationResult result =
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request("00123")
                );

        assertThat(result.accepted()).isTrue();
        assertThat(result.response().jobId())
                .isEqualTo("9001");

        verify(repository, never()).createPending(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void sameSucceededRequestReplaysExistingJobAsCompleted() {
        String requestHash =
                hasher.hashPurchaseUtility("123");

        when(repository.findByUserAndIdempotencyKey(
                USER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(
                Optional.of(
                        job(
                                9001L,
                                AiJobStatus.SUCCEEDED,
                                requestHash
                        )
                )
        );

        AiJobService.CreationResult result =
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request("123")
                );

        assertThat(result.accepted()).isFalse();
        assertThat(result.response().status())
                .isEqualTo(AiJobStatus.SUCCEEDED);
    }

    @Test
    void sameKeyWithDifferentRequestIsConflict() {
        String existingHash =
                hasher.hashPurchaseUtility("123");

        when(repository.findByUserAndIdempotencyKey(
                USER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(
                Optional.of(
                        job(
                                9001L,
                                AiJobStatus.PENDING,
                                existingHash
                        )
                )
        );

        assertThatThrownBy(() ->
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request("124")
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode
                                        .IDEMPOTENCY_KEY_CONFLICT
                        )
                );
    }

    @Test
    void duplicateInsertRaceReplaysWinner() {
        String requestHash =
                hasher.hashPurchaseUtility("123");

        AiJobData winner =
                job(
                        9002L,
                        AiJobStatus.PENDING,
                        requestHash
                );

        when(repository.findByUserAndIdempotencyKey(
                USER_ID,
                IDEMPOTENCY_KEY
        ))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));

        when(repository.createPending(
                USER_ID,
                AiJobType.PURCHASE_UTILITY,
                IDEMPOTENCY_KEY,
                requestHash,
                "test-model",
                "purchase-utility-summary-v1"
        )).thenThrow(
                new DuplicateKeyException(
                        "duplicate"
                )
        );

        AiJobService.CreationResult result =
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request("123")
                );

        assertThat(result.response().jobId())
                .isEqualTo("9002");
        assertThat(result.accepted()).isTrue();
    }

    private AiJobCreateRequest request(
            String productId
    ) {
        return new AiJobCreateRequest(
                AiJobType.PURCHASE_UTILITY,
                new AiJobCreateRequest.Context(
                        productId
                )
        );
    }

    private AiJobData job(
            Long jobId,
            AiJobStatus status,
            String requestHash
    ) {
        Instant createdAt =
                Instant.parse(
                        "2026-08-17T00:00:00Z"
                );

        return new AiJobData(
                jobId,
                USER_ID,
                AiJobType.PURCHASE_UTILITY,
                status,
                IDEMPOTENCY_KEY,
                requestHash,
                "test-model",
                "purchase-utility-summary-v1",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                status == AiJobStatus.PROCESSING
                        ? createdAt
                        : null,
                status == AiJobStatus.SUCCEEDED
                        || status == AiJobStatus.FAILED
                        ? createdAt.plusSeconds(3)
                        : null,
                createdAt,
                createdAt
        );
    }
}
