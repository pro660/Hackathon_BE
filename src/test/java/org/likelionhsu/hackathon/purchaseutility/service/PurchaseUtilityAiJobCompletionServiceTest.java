package org.likelionhsu.hackathon.purchaseutility.service;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiJobGateway;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationResult;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseUtilityAiJobCompletionServiceTest {

    @Mock PurchaseUtilityAiJobGateway aiJobGateway;
    @Mock PurchaseUtilityAnalysisFinalizationService finalizationService;

    private PurchaseUtilityAiJobCompletionService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseUtilityAiJobCompletionService(
                aiJobGateway,
                finalizationService
        );
    }

    @Test
    void aiCompletionUpdatesAnalysisAndJob() {
        PurchaseUtilityExplanationResult explanation =
                new PurchaseUtilityExplanationResult(
                        "AI 설명",
                        120,
                        45,
                        800L
                );

        when(aiJobGateway.markSucceeded(
                1L,
                900L,
                "{\"status\":\"READY\",\"analysisId\":\"801\"}",
                120,
                45,
                800L,
                1
        )).thenReturn(true);

        service.completeReadyWithAi(
                1L,
                900L,
                801L,
                explanation,
                1
        );

        verify(finalizationService)
                .applyAiExplanation(
                        1L,
                        801L,
                        "AI 설명"
                );
    }

    @Test
    void fallbackCompletionKeepsReadyCoreResult() {
        when(aiJobGateway.markFailed(
                eq(1L),
                eq(900L),
                contains("\"analysisId\":\"801\""),
                isNull(),
                eq(1),
                isNull()
        )).thenReturn(true);

        service.completeReadyWithFallback(
                1L,
                900L,
                801L,
                new BigDecimal("77.00"),
                null
        );

        verify(aiJobGateway)
                .markFailed(
                        eq(1L),
                        eq(900L),
                        contains("\"utilityScore\":77.00"),
                        isNull(),
                        eq(1),
                        isNull()
                );
    }
}
