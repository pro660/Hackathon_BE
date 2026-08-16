package org.likelionhsu.hackathon.purchaseutility.service;

import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.repository.PurchaseUtilityAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseUtilityAnalysisFinalizationService {

    private final PurchaseUtilityAnalysisRepository analysisRepository;

    public PurchaseUtilityAnalysisFinalizationService(
            PurchaseUtilityAnalysisRepository analysisRepository
    ) {
        this.analysisRepository = analysisRepository;
    }

    @Transactional
    public void applyAiExplanation(
            Long userId,
            Long analysisId,
            String summary
    ) {
        PurchaseUtilityAnalysis analysis =
                analysisRepository
                        .findByIdAndUser_Id(
                                analysisId,
                                userId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode
                                                .PURCHASE_UTILITY_ANALYSIS_NOT_FOUND
                                )
                        );

        analysis.applyAiExplanation(summary);
    }
}
