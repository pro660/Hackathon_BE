package org.likelionhsu.hackathon.purchaseutility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiInputHasher;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiJobGateway;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationPort;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationRequest;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationResult;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityExplanationGenerationType;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityAiJobProcessor.ProcessingOutcome;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class PurchaseUtilityAiJobProcessorTest {

    @Mock PurchaseUtilityAiJobGateway aiJobGateway;
    @Mock PurchaseUtilityAnalysisService analysisService;
    @Mock PurchaseUtilityAiInputHasher inputHasher;
    @Mock PurchaseUtilityAiSummaryCacheService cacheService;
    @Mock ObjectProvider<PurchaseUtilityExplanationPort>
            explanationPortProvider;
    @Mock PurchaseUtilityExplanationPort explanationPort;
    @Mock PurchaseUtilityAiJobCompletionService completionService;

    private PurchaseUtilityAiJobProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PurchaseUtilityAiJobProcessor(
                aiJobGateway,
                analysisService,
                inputHasher,
                cacheService,
                explanationPortProvider,
                completionService
        );
    }

    @Test
    void unclaimedJobDoesNothing() {
        when(aiJobGateway.claimProcessing(1L, 900L))
                .thenReturn(false);

        var result = processor.process(
                1L,
                900L,
                101L,
                "ko"
        );

        assertThat(result.outcome())
                .isEqualTo(ProcessingOutcome.NOT_CLAIMED);

        verifyNoInteractions(
                analysisService,
                inputHasher,
                cacheService,
                explanationPortProvider,
                completionService
        );
    }

    @Test
    void insufficientDataCompletesWithoutAiCall() {
        when(aiJobGateway.claimProcessing(1L, 900L))
                .thenReturn(true);
        when(analysisService.createRuleBasedAnalysis(
                1L,
                101L,
                900L
        )).thenReturn(
                PurchaseUtilityAnalysisService
                        .RuleAnalysisResult
                        .insufficientData()
        );

        var result = processor.process(
                1L,
                900L,
                101L,
                "ko"
        );

        assertThat(result.outcome())
                .isEqualTo(
                        ProcessingOutcome.INSUFFICIENT_DATA
                );

        verify(completionService)
                .completeInsufficient(
                        1L,
                        900L,
                        "활용 가능성을 분석하기 위한 정보가 부족해요."
                );
        verifyNoInteractions(
                inputHasher,
                cacheService,
                explanationPortProvider,
                explanationPort
        );
    }

    @Test
    void successfulAiExplanationCompletesReady() throws Exception {
        PurchaseUtilityAnalysis analysis = analysis(801L);

        when(aiJobGateway.claimProcessing(1L, 900L))
                .thenReturn(true);
        when(analysisService.createRuleBasedAnalysis(
                1L,
                101L,
                900L
        )).thenReturn(
                PurchaseUtilityAnalysisService
                        .RuleAnalysisResult
                        .ready(analysis)
        );

        stubCacheMiss();

        when(explanationPortProvider.getIfAvailable())
                .thenReturn(explanationPort);

        PurchaseUtilityExplanationResult explanation =
                new PurchaseUtilityExplanationResult(
                        "AI 설명",
                        120,
                        45,
                        800L
                );

        when(explanationPort.generate(
                any(PurchaseUtilityExplanationRequest.class)
        )).thenReturn(explanation);

        var result = processor.process(
                1L,
                900L,
                101L,
                "ko"
        );

        assertThat(result.outcome())
                .isEqualTo(ProcessingOutcome.READY_AI);
        assertThat(result.analysisId()).isEqualTo(801L);

        verify(completionService)
                .completeReadyWithAi(
                        1L,
                        900L,
                        801L,
                        explanation,
                        0
                );
    }

    @Test
    void firstAiFailureRetriesOnceAndCanSucceed() throws Exception {
        PurchaseUtilityAnalysis analysis = analysis(801L);

        when(aiJobGateway.claimProcessing(1L, 900L))
                .thenReturn(true);
        when(analysisService.createRuleBasedAnalysis(
                1L,
                101L,
                900L
        )).thenReturn(
                PurchaseUtilityAnalysisService
                        .RuleAnalysisResult
                        .ready(analysis)
        );

        stubCacheMiss();

        when(explanationPortProvider.getIfAvailable())
                .thenReturn(explanationPort);

        PurchaseUtilityExplanationResult explanation =
                new PurchaseUtilityExplanationResult(
                        "재시도 성공",
                        null,
                        null,
                        null
                );

        when(explanationPort.generate(
                any(PurchaseUtilityExplanationRequest.class)
        ))
                .thenThrow(new RuntimeException("first"))
                .thenReturn(explanation);

        var result = processor.process(
                1L,
                900L,
                101L,
                "ko"
        );

        assertThat(result.outcome())
                .isEqualTo(ProcessingOutcome.READY_AI);

        verify(completionService)
                .completeReadyWithAi(
                        1L,
                        900L,
                        801L,
                        explanation,
                        1
                );
    }

    @Test
    void recentMatchingSummaryCompletesWithoutAiCall()
            throws Exception {
        PurchaseUtilityAnalysis analysis = analysis(801L);

        when(aiJobGateway.claimProcessing(1L, 900L))
                .thenReturn(true);
        when(analysisService.createRuleBasedAnalysis(
                1L,
                101L,
                900L
        )).thenReturn(
                PurchaseUtilityAnalysisService
                        .RuleAnalysisResult
                        .ready(analysis)
        );
        when(inputHasher.hash(
                any(PurchaseUtilityExplanationRequest.class)
        )).thenReturn("a".repeat(64));
        when(cacheService.storeInputHashAndFindReusableSummary(
                1L,
                900L,
                "a".repeat(64)
        )).thenReturn(Optional.of("캐시된 AI 설명"));

        var result = processor.process(
                1L,
                900L,
                101L,
                "ko"
        );

        assertThat(result.outcome())
                .isEqualTo(ProcessingOutcome.READY_CACHED);
        assertThat(result.analysisId()).isEqualTo(801L);

        verify(completionService)
                .completeReadyWithAi(
                        1L,
                        900L,
                        801L,
                        new PurchaseUtilityExplanationResult(
                                "캐시된 AI 설명",
                                null,
                                null,
                                null
                        ),
                        0
                );
        verifyNoInteractions(
                explanationPortProvider,
                explanationPort
        );
    }

    @Test
    void missingAiAdapterKeepsRuleBasedAnalysisWithoutRetry()
            throws Exception {
        PurchaseUtilityAnalysis analysis = analysis(801L);

        when(aiJobGateway.claimProcessing(1L, 900L))
                .thenReturn(true);
        when(analysisService.createRuleBasedAnalysis(
                1L,
                101L,
                900L
        )).thenReturn(
                PurchaseUtilityAnalysisService
                        .RuleAnalysisResult
                        .ready(analysis)
        );

        stubCacheMiss();

        var result = processor.process(
                1L,
                900L,
                101L,
                "ko"
        );

        assertThat(result.outcome())
                .isEqualTo(
                        ProcessingOutcome
                                .READY_RULE_BASED_FALLBACK
                );
        assertThat(result.analysisId()).isEqualTo(801L);

        verify(completionService)
                .completeReadyWithFallback(
                        1L,
                        900L,
                        801L,
                        new BigDecimal("77.00"),
                        "AI_GENERATION_FAILED",
                        0
                );
        verifyNoInteractions(explanationPort);
    }

    @Test
    void repeatedAiFailureKeepsRuleBasedAnalysis() throws Exception {
        PurchaseUtilityAnalysis analysis = analysis(801L);

        when(aiJobGateway.claimProcessing(1L, 900L))
                .thenReturn(true);
        when(analysisService.createRuleBasedAnalysis(
                1L,
                101L,
                900L
        )).thenReturn(
                PurchaseUtilityAnalysisService
                        .RuleAnalysisResult
                        .ready(analysis)
        );

        stubCacheMiss();

        when(explanationPortProvider.getIfAvailable())
                .thenReturn(explanationPort);
        when(explanationPort.generate(
                any(PurchaseUtilityExplanationRequest.class)
        ))
                .thenThrow(new RuntimeException("first"))
                .thenThrow(new RuntimeException("second"));

        var result = processor.process(
                1L,
                900L,
                101L,
                "ko"
        );

        assertThat(result.outcome())
                .isEqualTo(
                        ProcessingOutcome
                                .READY_RULE_BASED_FALLBACK
                );
        assertThat(result.analysisId()).isEqualTo(801L);

        verify(completionService)
                .completeReadyWithFallback(
                        1L,
                        900L,
                        801L,
                        new BigDecimal("77.00"),
                        "AI_GENERATION_FAILED",
                        1
                );
    }

    private void stubCacheMiss() {
        when(inputHasher.hash(
                any(PurchaseUtilityExplanationRequest.class)
        )).thenReturn("a".repeat(64));
        when(cacheService.storeInputHashAndFindReusableSummary(
                1L,
                900L,
                "a".repeat(64)
        )).thenReturn(Optional.empty());
    }

    private PurchaseUtilityAnalysis analysis(
            Long id
    ) throws Exception {
        Product product = mock(Product.class);

        when(product.getId()).thenReturn(101L);
        when(product.getName()).thenReturn("Aren Shopper");
        when(product.getCategory())
                .thenReturn(ItemCategory.BAG);
        when(product.getPrimaryColor())
                .thenReturn(ColorGroup.BROWN);

        PurchaseUtilityFactorSnapshot factors =
                new PurchaseUtilityFactorSnapshot(
                        "purchase-utility-rule-v1",
                        PurchaseUtilityExplanationGenerationType.RULE_BASED,
                        new PurchaseUtilityFactorSnapshot.PreferenceFactor(
                                new BigDecimal("20.00"),
                                new BigDecimal("30.00"),
                                true,
                                true,
                                false
                        ),
                        new PurchaseUtilityFactorSnapshot.ItemCombinationFactor(
                                new BigDecimal("18.00"),
                                new BigDecimal("25.00"),
                                0,
                                List.of()
                        ),
                        new PurchaseUtilityFactorSnapshot.SeasonFactor(
                                new BigDecimal("25.00"),
                                new BigDecimal("25.00"),
                                4,
                                true
                        ),
                        new PurchaseUtilityFactorSnapshot
                                .CategoryCombinationFactor(
                                new BigDecimal("14.00"),
                                new BigDecimal("20.00"),
                                2
                        )
                );

        PurchaseUtilityAnalysis analysis =
                PurchaseUtilityAnalysis.createRuleBased(
                        mock(User.class),
                        product,
                        new BigDecimal("77.00"),
                        0,
                        factors,
                        "규칙 기반 분석",
                        900L,
                        Instant.parse("2026-08-16T00:00:00Z")
                );

        Field idField =
                PurchaseUtilityAnalysis.class
                        .getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(analysis, id);

        return analysis;
    }
}
