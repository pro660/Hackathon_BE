package org.likelionhsu.hackathon.purchaseutility.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.purchaseutility.domain.CareDifficulty;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityExplanationGenerationType;

public record PurchaseUtilityAnalysisResponse(
        String analysisId,
        String scorePolicyVersion,
        ProductResponse product,
        BigDecimal utilityScore,
        FactorScoresResponse factors,
        int compatibleItemCount,
        List<CompatibleItemResponse> compatibleItems,
        CareDifficulty careDifficulty,
        String summary,
        PurchaseUtilityExplanationGenerationType explanationGenerationType,
        Instant analyzedAt
) {

    public PurchaseUtilityAnalysisResponse {
        compatibleItems = List.copyOf(compatibleItems);
    }

    public record ProductResponse(
            String productId,
            String name,
            ItemCategory category,
            long price,
            String primaryImageUrl
    ) {
    }

    public record FactorScoresResponse(
            BigDecimal preferenceTagFitScore,
            BigDecimal styleCombinationScore,
            BigDecimal seasonUsabilityScore,
            BigDecimal ownedCategoryCombinationScore
    ) {
    }

    public record CompatibleItemResponse(
            String myItemId,
            String name,
            String imageUrl,
            String reason
    ) {
    }
}
