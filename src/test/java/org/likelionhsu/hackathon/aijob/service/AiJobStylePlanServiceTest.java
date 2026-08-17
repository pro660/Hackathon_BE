package org.likelionhsu.hackathon.aijob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
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
import org.likelionhsu.hackathon.itemanalysis.service.ItemAnalysisAiJobDispatcher;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityAiJobDispatcher;
import org.likelionhsu.hackathon.styleplan.service.StylePlanAiJobDispatcher;
import org.likelionhsu.hackathon.styleplan.service.StylePlanJobRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiJobStylePlanServiceTest {

    private static final Long USER_ID = 1L;
    private static final String IDEMPOTENCY_KEY =
            "style-plan-key";

    @Mock
    private AiJobJdbcRepository repository;
    @Mock
    private ItemAnalysisAiJobCreationService creationService;
    @Mock
    private ItemAnalysisAiJobDispatcher itemDispatcher;
    @Mock
    private PurchaseUtilityAiJobDispatcher purchaseDispatcher;
    @Mock
    private StylePlanAiJobDispatcher styleDispatcher;

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
                itemDispatcher,
                purchaseDispatcher,
                styleDispatcher,
                "test-model"
        );
    }

    @Test
    void newStylePlanJobIsCreatedAndDispatched() {
        AiJobCreateRequest request = request();
        StylePlanJobRequest normalized =
                StylePlanJobRequest.from(request);
        String requestHash = hasher.hashStylePlan(
                normalized.occasion(),
                normalized.styleTags(),
                normalized.weatherCondition(),
                normalized.prioritizeOwnedItems(),
                normalized.language()
        );

        when(repository.findByUserAndIdempotencyKey(
                USER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(Optional.empty());
        when(repository.createPending(
                USER_ID,
                AiJobType.STYLE_PLAN,
                IDEMPOTENCY_KEY,
                requestHash,
                "test-model",
                "style-plan-v1"
        )).thenReturn(9201L);
        when(repository.findOwned(
                USER_ID,
                9201L
        )).thenReturn(Optional.of(
                job(9201L, requestHash)
        ));

        AiJobService.CreationResult result =
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request
                );

        assertThat(result.accepted()).isTrue();
        assertThat(result.response().jobId())
                .isEqualTo("9201");
        assertThat(result.response().type())
                .isEqualTo(AiJobType.STYLE_PLAN);

        verify(styleDispatcher).dispatch(
                USER_ID,
                9201L,
                normalized
        );
        verifyNoInteractions(itemDispatcher);
        verifyNoInteractions(purchaseDispatcher);
    }

    @Test
    void sameStylePlanRequestReplaysExistingJob() {
        StylePlanJobRequest normalized =
                StylePlanJobRequest.from(request());
        String requestHash = hasher.hashStylePlan(
                normalized.occasion(),
                normalized.styleTags(),
                normalized.weatherCondition(),
                normalized.prioritizeOwnedItems(),
                normalized.language()
        );

        AiJobData existing = new AiJobData(
                9201L,
                USER_ID,
                AiJobType.STYLE_PLAN,
                AiJobStatus.SUCCEEDED,
                IDEMPOTENCY_KEY,
                requestHash,
                "test-model",
                "style-plan-v1",
                null,
                "{}",
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                Instant.parse("2026-08-18T00:00:10Z"),
                Instant.parse("2026-08-18T00:00:00Z"),
                Instant.parse("2026-08-18T00:00:10Z")
        );

        when(repository.findByUserAndIdempotencyKey(
                USER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(Optional.of(existing));

        AiJobService.CreationResult result =
                service.create(
                        USER_ID,
                        IDEMPOTENCY_KEY,
                        request()
                );

        assertThat(result.accepted()).isFalse();
        verifyNoInteractions(styleDispatcher);
    }

    private AiJobCreateRequest request() {
        return new AiJobCreateRequest(
                AiJobType.STYLE_PLAN,
                new AiJobCreateRequest.Context(
                        null,
                        null,
                        "DATE",
                        List.of("GLAMOROUS", "NEAT"),
                        null,
                        true,
                        "ko"
                )
        );
    }

    private AiJobData job(
            Long jobId,
            String requestHash
    ) {
        Instant now =
                Instant.parse("2026-08-18T00:00:00Z");

        return new AiJobData(
                jobId,
                USER_ID,
                AiJobType.STYLE_PLAN,
                AiJobStatus.PENDING,
                IDEMPOTENCY_KEY,
                requestHash,
                "test-model",
                "style-plan-v1",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                now,
                now
        );
    }
}
