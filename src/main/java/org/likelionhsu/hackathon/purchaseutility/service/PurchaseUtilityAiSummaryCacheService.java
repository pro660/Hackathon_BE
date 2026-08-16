package org.likelionhsu.hackathon.purchaseutility.service;

import java.util.Optional;

import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiJobData;
import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityAiJobGateway;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityExplanationGenerationType;
import org.likelionhsu.hackathon.purchaseutility.repository.PurchaseUtilityAnalysisRepository;
import org.springframework.stereotype.Service;

@Service
public class PurchaseUtilityAiSummaryCacheService {

    private final PurchaseUtilityAiJobGateway aiJobGateway;
    private final PurchaseUtilityAnalysisRepository analysisRepository;

    public PurchaseUtilityAiSummaryCacheService(
            PurchaseUtilityAiJobGateway aiJobGateway,
            PurchaseUtilityAnalysisRepository analysisRepository
    ) {
        this.aiJobGateway = aiJobGateway;
        this.analysisRepository = analysisRepository;
    }

    public Optional<String> storeInputHashAndFindReusableSummary(
            Long userId,
            Long jobId,
            String inputHash
    ) {
        boolean stored = aiJobGateway.updateInputHashIfProcessing(
                userId,
                jobId,
                inputHash
        );

        if (!stored) {
            throw new IllegalStateException(
                    "AI Job input_hash 저장에 실패했습니다."
            );
        }

        PurchaseUtilityAiJobData currentJob = aiJobGateway
                .findOwned(userId, jobId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "처리 중인 AI Job을 찾을 수 없습니다."
                        )
                );

        return aiJobGateway
                .findRecentSucceededByInputHash(
                        userId,
                        inputHash,
                        currentJob.promptVersion(),
                        currentJob.model()
                )
                .flatMap(cachedJob ->
                        analysisRepository.findByAiJobIdAndUser_Id(
                                cachedJob.id(),
                                userId
                        )
                )
                .filter(this::hasAiGeneratedSummary)
                .map(PurchaseUtilityAnalysis::getSummary)
                .map(String::trim)
                .filter(summary -> !summary.isEmpty());
    }

    private boolean hasAiGeneratedSummary(
            PurchaseUtilityAnalysis analysis
    ) {
        return analysis.getFactorJson()
                .explanationGenerationType()
                == PurchaseUtilityExplanationGenerationType.AI
                && analysis.getSummary() != null;
    }
}
