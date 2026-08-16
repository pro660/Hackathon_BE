package org.likelionhsu.hackathon.purchaseutility.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityCompatibleItemSnapshot;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;

public record PurchaseUtilityExplanationRequest(
        String analysisId,
        String scorePolicyVersion,
        ProductContext product,
        BigDecimal utilityScore,
        FactorScores factors,
        int compatibleItemCount,
        List<CompatibleItemContext> compatibleItems,
        String language
) {

    public PurchaseUtilityExplanationRequest {
        compatibleItems = List.copyOf(compatibleItems);
    }

    public static PurchaseUtilityExplanationRequest from(
            PurchaseUtilityAnalysis analysis,
            String language
    ) {
        Objects.requireNonNull(
                analysis,
                "analysis는 null일 수 없습니다."
        );

        String normalizedLanguage =
                Objects.requireNonNull(
                        language,
                        "language는 null일 수 없습니다."
                ).trim();

        if (normalizedLanguage.isEmpty()) {
            throw new IllegalArgumentException(
                    "language는 비어 있을 수 없습니다."
            );
        }

        PurchaseUtilityFactorSnapshot factorJson =
                analysis.getFactorJson();

        List<CompatibleItemContext> compatibleItems =
                factorJson
                        .itemCombination()
                        .compatibleItems()
                        .stream()
                        .map(
                                PurchaseUtilityExplanationRequest
                                        ::toCompatibleItemContext
                        )
                        .toList();

        return new PurchaseUtilityExplanationRequest(
                String.valueOf(analysis.getId()),
                factorJson.ruleVersion(),
                new ProductContext(
                        String.valueOf(
                                analysis
                                        .getProduct()
                                        .getId()
                        ),
                        analysis.getProduct().getName(),
                        analysis.getProduct().getCategory(),
                        analysis.getProduct().getPrimaryColor()
                ),
                analysis.getUtilityScore(),
                new FactorScores(
                        factorJson.preference().score(),
                        factorJson.itemCombination().score(),
                        factorJson.season().score(),
                        factorJson.categoryCombination().score()
                ),
                analysis.getCompatibleItemCount(),
                compatibleItems,
                normalizedLanguage
        );
    }

    private static CompatibleItemContext
    toCompatibleItemContext(
            PurchaseUtilityCompatibleItemSnapshot item
    ) {
        return new CompatibleItemContext(
                item.myItemId(),
                item.name(),
                item.category(),
                item.primaryColor(),
                item.reason()
        );
    }

    public record ProductContext(
            String productId,
            String name,
            ItemCategory category,
            ColorGroup primaryColor
    ) {
    }

    public record FactorScores(
            BigDecimal preferenceTagFitScore,
            BigDecimal styleCombinationScore,
            BigDecimal seasonUsabilityScore,
            BigDecimal ownedCategoryCombinationScore
    ) {
    }

    public record CompatibleItemContext(
            String myItemId,
            String name,
            ItemCategory category,
            ColorGroup primaryColor,
            String reason
    ) {
    }
}
