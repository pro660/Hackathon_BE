package org.likelionhsu.hackathon.purchaseutility.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.preference.entity.PreferenceStyleTag;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityExplanationGenerationType;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityCompatibleItemSnapshot;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;
import org.springframework.stereotype.Component;

@Component
public class PurchaseUtilityScorer {

    public static final String RULE_VERSION =
            "purchase-utility-rule-v1";

    private static final BigDecimal PREFERENCE_MAX_SCORE =
            score("30.00");
    private static final BigDecimal ITEM_COMBINATION_MAX_SCORE =
            score("25.00");
    private static final BigDecimal SEASON_MAX_SCORE =
            score("25.00");
    private static final BigDecimal CATEGORY_COMBINATION_MAX_SCORE =
            score("20.00");

    private static final BigDecimal PREFERENCE_AXIS_SCORE =
            score("10.00");

    private static final Set<ColorGroup> NEUTRAL_COLORS = Set.of(
            ColorGroup.BLACK,
            ColorGroup.WHITE,
            ColorGroup.GRAY,
            ColorGroup.BROWN,
            ColorGroup.BEIGE
    );

    private static final Set<String> FOUR_SEASONS = Set.of(
            "SPRING",
            "SUMMER",
            "AUTUMN",
            "WINTER"
    );

    private static final String COMPATIBLE_REASON =
            "구매 후보 제품과 색상 조합이 가능한 아이템입니다.";

    public PurchaseUtilityScoreResult score(
            List<PreferenceStyleTag> preferredStyleTags,
            List<ItemCategory> preferredCategories,
            List<ColorGroup> preferredColors,
            ItemCategory productCategory,
            ColorGroup productPrimaryColor,
            List<String> productStyleTags,
            List<String> productSeasonTags,
            List<UserItemCandidate> userItems
    ) {
        Set<String> styles = Set.copyOf(productStyleTags);
        Set<String> seasons = Set.copyOf(productSeasonTags);

        boolean styleMatched = preferredStyleTags
                .stream()
                .map(Enum::name)
                .anyMatch(styles::contains);
        boolean categoryMatched =
                preferredCategories.contains(productCategory);
        boolean colorMatched =
                productPrimaryColor != null
                        && preferredColors.contains(productPrimaryColor);

        BigDecimal preferenceScore = zero();
        if (styleMatched) {
            preferenceScore = preferenceScore.add(
                    PREFERENCE_AXIS_SCORE
            );
        }
        if (categoryMatched) {
            preferenceScore = preferenceScore.add(
                    PREFERENCE_AXIS_SCORE
            );
        }
        if (colorMatched) {
            preferenceScore = preferenceScore.add(
                    PREFERENCE_AXIS_SCORE
            );
        }

        List<PurchaseUtilityCompatibleItemSnapshot> compatibleItems =
                userItems
                        .stream()
                        .filter(item -> isColorCompatible(
                                productPrimaryColor,
                                item.primaryColor()
                        ))
                        .map(this::toCompatibleItemSnapshot)
                        .toList();

        int compatibleItemCount = compatibleItems.size();
        BigDecimal itemCombinationScore =
                calculateItemCombinationScore(
                        compatibleItemCount
                );

        int complementaryCategoryCount = (int) userItems
                .stream()
                .map(UserItemCandidate::category)
                .filter(category -> category != productCategory)
                .distinct()
                .count();
        BigDecimal categoryCombinationScore =
                calculateCategoryCombinationScore(
                        complementaryCategoryCount
                );

        boolean allSeason = seasons.contains("ALL_SEASON");
        int seasonCount = allSeason
                ? FOUR_SEASONS.size()
                : (int) seasons
                        .stream()
                        .filter(FOUR_SEASONS::contains)
                        .distinct()
                        .count();
        BigDecimal seasonScore =
                calculateSeasonScore(
                        seasonCount,
                        allSeason
                );

        PurchaseUtilityFactorSnapshot factorSnapshot =
                new PurchaseUtilityFactorSnapshot(
                        RULE_VERSION,
                        PurchaseUtilityExplanationGenerationType.RULE_BASED,
                        new PurchaseUtilityFactorSnapshot.PreferenceFactor(
                                preferenceScore,
                                PREFERENCE_MAX_SCORE,
                                styleMatched,
                                categoryMatched,
                                colorMatched
                        ),
                        new PurchaseUtilityFactorSnapshot.ItemCombinationFactor(
                                itemCombinationScore,
                                ITEM_COMBINATION_MAX_SCORE,
                                compatibleItemCount,
                                compatibleItems
                        ),
                        new PurchaseUtilityFactorSnapshot.SeasonFactor(
                                seasonScore,
                                SEASON_MAX_SCORE,
                                seasonCount,
                                allSeason
                        ),
                        new PurchaseUtilityFactorSnapshot.CategoryCombinationFactor(
                                categoryCombinationScore,
                                CATEGORY_COMBINATION_MAX_SCORE,
                                complementaryCategoryCount
                        )
                );

        BigDecimal total = preferenceScore
                .add(itemCombinationScore)
                .add(seasonScore)
                .add(categoryCombinationScore)
                .setScale(2, RoundingMode.HALF_UP);

        return new PurchaseUtilityScoreResult(
                total,
                factorSnapshot
        );
    }

    private boolean isColorCompatible(
            ColorGroup productColor,
            ColorGroup itemColor
    ) {
        if (productColor == null || itemColor == null) {
            return false;
        }

        if (productColor == itemColor) {
            return true;
        }

        if (productColor == ColorGroup.OTHER
                || itemColor == ColorGroup.OTHER) {
            return false;
        }

        if (productColor == ColorGroup.MULTI
                || itemColor == ColorGroup.MULTI) {
            return true;
        }

        return NEUTRAL_COLORS.contains(productColor)
                || NEUTRAL_COLORS.contains(itemColor);
    }

    private PurchaseUtilityCompatibleItemSnapshot
    toCompatibleItemSnapshot(
            UserItemCandidate item
    ) {
        return new PurchaseUtilityCompatibleItemSnapshot(
                String.valueOf(item.myItemId()),
                item.name(),
                item.category(),
                item.primaryColor(),
                item.imageUrl(),
                COMPATIBLE_REASON
        );
    }

    private BigDecimal calculateItemCombinationScore(
            int compatibleItemCount
    ) {
        return switch (compatibleItemCount) {
            case 0 -> zero();
            case 1 -> score("10.00");
            case 2 -> score("18.00");
            default -> score("25.00");
        };
    }

    private BigDecimal calculateSeasonScore(
            int seasonCount,
            boolean allSeason
    ) {
        if (allSeason || seasonCount >= 4) {
            return score("25.00");
        }

        return switch (seasonCount) {
            case 0 -> zero();
            case 1 -> score("10.00");
            case 2 -> score("15.00");
            case 3 -> score("20.00");
            default -> score("25.00");
        };
    }

    private BigDecimal calculateCategoryCombinationScore(
            int complementaryCategoryCount
    ) {
        return switch (complementaryCategoryCount) {
            case 0 -> zero();
            case 1 -> score("8.00");
            case 2 -> score("14.00");
            default -> score("20.00");
        };
    }

    private static BigDecimal score(String value) {
        return new BigDecimal(value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero() {
        return score("0.00");
    }

    public record UserItemCandidate(
            Long myItemId,
            String name,
            ItemCategory category,
            ColorGroup primaryColor,
            String imageUrl
    ) {
    }

    public record PurchaseUtilityScoreResult(
            BigDecimal total,
            PurchaseUtilityFactorSnapshot factors
    ) {
        public int compatibleItemCount() {
            return factors
                    .itemCombination()
                    .compatibleItemCount();
        }
    }
}
