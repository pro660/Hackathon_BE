package org.likelionhsu.hackathon.purchaseutility.entity.snapshot;

import java.math.BigDecimal;
import java.util.List;

import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityExplanationGenerationType;

public record PurchaseUtilityFactorSnapshot(
        String ruleVersion,
        PurchaseUtilityExplanationGenerationType explanationGenerationType,
        PreferenceFactor preference,
        ItemCombinationFactor itemCombination,
        SeasonFactor season,
        CategoryCombinationFactor categoryCombination
) {

    public record PreferenceFactor(
            BigDecimal score,
            BigDecimal maxScore,
            boolean styleMatched,
            boolean categoryMatched,
            boolean colorMatched
    ) {
    }

    public record ItemCombinationFactor(
            BigDecimal score,
            BigDecimal maxScore,
            int compatibleItemCount,
            List<PurchaseUtilityCompatibleItemSnapshot> compatibleItems
    ) {
        public ItemCombinationFactor {
            compatibleItems = List.copyOf(compatibleItems);
        }
    }

    public record SeasonFactor(
            BigDecimal score,
            BigDecimal maxScore,
            int seasonCount,
            boolean allSeason
    ) {
    }

    public record CategoryCombinationFactor(
            BigDecimal score,
            BigDecimal maxScore,
            int complementaryCategoryCount
    ) {
    }
}
