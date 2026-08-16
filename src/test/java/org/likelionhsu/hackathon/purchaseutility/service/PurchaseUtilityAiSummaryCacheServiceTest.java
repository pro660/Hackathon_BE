package org.likelionhsu.hackathon.purchaseutility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiJobData;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiJobGateway;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityExplanationGenerationType;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;
import org.likelionhsu.hackathon.purchaseutility.repository.PurchaseUtilityAnalysisRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseUtilityAiSummaryCacheServiceTest {

    private static final String INPUT_HASH = "a".repeat(64);

    @Mock PurchaseUtilityAiJobGateway aiJobGateway;
    @Mock PurchaseUtilityAnalysisRepository analysisRepository;

    private PurchaseUtilityAiSummaryCacheService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseUtilityAiSummaryCacheService(
                aiJobGateway,
                analysisRepository
        );
    }

    @Test
    void matchingRecentAiSummaryCanBeReused() {
        PurchaseUtilityAnalysis analysis = cachedAnalysis(
                PurchaseUtilityExplanationGenerationType.AI,
                "  캐시된 AI 설명  "
        );
        stubCacheCandidate(analysis);

        Optional<String> result =
                service.storeInputHashAndFindReusableSummary(
                        1L,
                        900L,
                        INPUT_HASH
                );

        assertThat(result).contains("캐시된 AI 설명");
    }

    @Test
    void ruleBasedSummaryIsNotReused() {
        PurchaseUtilityAnalysis analysis = cachedAnalysis(
                PurchaseUtilityExplanationGenerationType.RULE_BASED,
                "규칙 기반 설명"
        );
        stubCacheCandidate(analysis);

        Optional<String> result =
                service.storeInputHashAndFindReusableSummary(
                        1L,
                        900L,
                        INPUT_HASH
                );

        assertThat(result).isEmpty();
    }

    @Test
    void inputHashMustBeStoredBeforeCacheLookup() {
        when(aiJobGateway.updateInputHashIfProcessing(
                1L,
                900L,
                INPUT_HASH
        )).thenReturn(false);

        assertThatThrownBy(() ->
                service.storeInputHashAndFindReusableSummary(
                        1L,
                        900L,
                        INPUT_HASH
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI Job input_hash 저장에 실패했습니다.");
    }

    private void stubCacheCandidate(
            PurchaseUtilityAnalysis analysis
    ) {
        PurchaseUtilityAiJobData currentJob =
                mock(PurchaseUtilityAiJobData.class);
        PurchaseUtilityAiJobData cachedJob =
                mock(PurchaseUtilityAiJobData.class);

        when(aiJobGateway.updateInputHashIfProcessing(
                1L,
                900L,
                INPUT_HASH
        )).thenReturn(true);
        when(aiJobGateway.findOwned(1L, 900L))
                .thenReturn(Optional.of(currentJob));
        when(currentJob.promptVersion())
                .thenReturn("purchase-utility-summary-v1");
        when(currentJob.model())
                .thenReturn("configured-model");
        when(aiJobGateway.findRecentSucceededByInputHash(
                1L,
                INPUT_HASH,
                "purchase-utility-summary-v1",
                "configured-model"
        )).thenReturn(Optional.of(cachedJob));
        when(cachedJob.id()).thenReturn(700L);
        when(analysisRepository.findByAiJobIdAndUser_Id(
                700L,
                1L
        )).thenReturn(Optional.of(analysis));
    }

    private PurchaseUtilityAnalysis cachedAnalysis(
            PurchaseUtilityExplanationGenerationType generationType,
            String summary
    ) {
        PurchaseUtilityAnalysis analysis =
                mock(PurchaseUtilityAnalysis.class);
        when(analysis.getFactorJson())
                .thenReturn(factors(generationType));

        if (generationType
                == PurchaseUtilityExplanationGenerationType.AI) {
            when(analysis.getSummary()).thenReturn(summary);
        }

        return analysis;
    }

    private PurchaseUtilityFactorSnapshot factors(
            PurchaseUtilityExplanationGenerationType generationType
    ) {
        return new PurchaseUtilityFactorSnapshot(
                "purchase-utility-rule-v1",
                generationType,
                new PurchaseUtilityFactorSnapshot.PreferenceFactor(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        false,
                        false,
                        false
                ),
                new PurchaseUtilityFactorSnapshot.ItemCombinationFactor(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0,
                        List.of()
                ),
                new PurchaseUtilityFactorSnapshot.SeasonFactor(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0,
                        false
                ),
                new PurchaseUtilityFactorSnapshot
                        .CategoryCombinationFactor(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0
                )
        );
    }
}
