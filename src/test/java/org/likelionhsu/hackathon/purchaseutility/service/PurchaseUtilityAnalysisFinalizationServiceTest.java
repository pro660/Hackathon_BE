package org.likelionhsu.hackathon.purchaseutility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.repository.PurchaseUtilityAnalysisRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseUtilityAnalysisFinalizationServiceTest {

    @Mock PurchaseUtilityAnalysisRepository analysisRepository;

    private PurchaseUtilityAnalysisFinalizationService service;

    @BeforeEach
    void setUp() {
        service =
                new PurchaseUtilityAnalysisFinalizationService(
                        analysisRepository
                );
    }

    @Test
    void appliesAiExplanationOnlyToOwnedAnalysis() {
        PurchaseUtilityAnalysis analysis =
                mock(PurchaseUtilityAnalysis.class);

        when(analysisRepository.findByIdAndUser_Id(
                801L,
                1L
        )).thenReturn(Optional.of(analysis));

        service.applyAiExplanation(
                1L,
                801L,
                "AI 설명"
        );

        verify(analysis)
                .applyAiExplanation("AI 설명");
    }

    @Test
    void missingOrForeignAnalysisUsesSameNotFoundError() {
        when(analysisRepository.findByIdAndUser_Id(
                999L,
                1L
        )).thenReturn(Optional.empty());

        BusinessException exception =
                catchThrowableOfType(
                        () ->
                                service.applyAiExplanation(
                                        1L,
                                        999L,
                                        "AI 설명"
                                ),
                        BusinessException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        ErrorCode
                                .PURCHASE_UTILITY_ANALYSIS_NOT_FOUND
                );
    }
}
