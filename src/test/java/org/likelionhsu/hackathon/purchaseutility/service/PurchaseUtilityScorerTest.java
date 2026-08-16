package org.likelionhsu.hackathon.purchaseutility.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.preference.entity.PreferenceStyleTag;

class PurchaseUtilityScorerTest {

    private final PurchaseUtilityScorer scorer =
            new PurchaseUtilityScorer();

    @Test
    void fullMatchScoresOneHundred() {
        var result = scorer.score(
                List.of(PreferenceStyleTag.CASUAL),
                List.of(ItemCategory.BAG),
                List.of(ColorGroup.RED),
                ItemCategory.BAG,
                ColorGroup.RED,
                List.of("CASUAL"),
                List.of("ALL_SEASON"),
                List.of(
                        candidate(
                                1L,
                                "블랙 재킷",
                                ItemCategory.CLOTHING,
                                ColorGroup.BLACK
                        ),
                        candidate(
                                2L,
                                "베이지 슈즈",
                                ItemCategory.SHOES,
                                ColorGroup.BEIGE
                        ),
                        candidate(
                                3L,
                                "레드 액세서리",
                                ItemCategory.FASHION_ACCESSORY,
                                ColorGroup.RED
                        )
                )
        );

        assertThat(result.total())
                .isEqualByComparingTo("100.00");
        assertThat(result.factors().preference().score())
                .isEqualByComparingTo("30.00");
        assertThat(result.factors().itemCombination().score())
                .isEqualByComparingTo("25.00");
        assertThat(result.factors().season().score())
                .isEqualByComparingTo("25.00");
        assertThat(result.factors().categoryCombination().score())
                .isEqualByComparingTo("20.00");
        assertThat(result.compatibleItemCount()).isEqualTo(3);
    }

    @Test
    void preferenceAxesScoreIndependently() {
        var result = scorer.score(
                List.of(PreferenceStyleTag.CASUAL),
                List.of(ItemCategory.BAG),
                List.of(ColorGroup.BLACK),
                ItemCategory.BAG,
                ColorGroup.RED,
                List.of("CASUAL"),
                List.of("WINTER"),
                List.of()
        );

        assertThat(result.factors().preference().score())
                .isEqualByComparingTo("20.00");
        assertThat(result.factors().preference().styleMatched())
                .isTrue();
        assertThat(result.factors().preference().categoryMatched())
                .isTrue();
        assertThat(result.factors().preference().colorMatched())
                .isFalse();
    }

    @Test
    void colorCompatibilityUsesNeutralMultiSameAndOtherRules() {
        var result = scorer.score(
                List.of(PreferenceStyleTag.FORMAL),
                List.of(ItemCategory.LEATHER_GOODS),
                List.of(ColorGroup.BLUE),
                ItemCategory.BAG,
                ColorGroup.RED,
                List.of("CASUAL"),
                List.of("WINTER"),
                List.of(
                        candidate(
                                1L,
                                "블랙",
                                ItemCategory.CLOTHING,
                                ColorGroup.BLACK
                        ),
                        candidate(
                                2L,
                                "레드",
                                ItemCategory.SHOES,
                                ColorGroup.RED
                        ),
                        candidate(
                                3L,
                                "멀티",
                                ItemCategory.FASHION_ACCESSORY,
                                ColorGroup.MULTI
                        ),
                        candidate(
                                4L,
                                "블루",
                                ItemCategory.CLOTHING,
                                ColorGroup.BLUE
                        ),
                        candidate(
                                5L,
                                "기타",
                                ItemCategory.SHOES,
                                ColorGroup.OTHER
                        ),
                        candidate(
                                6L,
                                "색상없음",
                                ItemCategory.BAG,
                                null
                        )
                )
        );

        assertThat(result.compatibleItemCount()).isEqualTo(3);
        assertThat(result.factors().itemCombination().score())
                .isEqualByComparingTo("25.00");
        assertThat(result.factors()
                .itemCombination()
                .compatibleItems())
                .extracting(item -> item.myItemId())
                .containsExactly("1", "2", "3");
    }

    @Test
    void twoCompatibleItemsScoreEighteen() {
        var result = scorer.score(
                List.of(PreferenceStyleTag.FORMAL),
                List.of(ItemCategory.LEATHER_GOODS),
                List.of(ColorGroup.BLUE),
                ItemCategory.BAG,
                ColorGroup.RED,
                List.of(),
                List.of("SPRING"),
                List.of(
                        candidate(
                                1L,
                                "블랙",
                                ItemCategory.CLOTHING,
                                ColorGroup.BLACK
                        ),
                        candidate(
                                2L,
                                "레드",
                                ItemCategory.SHOES,
                                ColorGroup.RED
                        ),
                        candidate(
                                3L,
                                "블루",
                                ItemCategory.CLOTHING,
                                ColorGroup.BLUE
                        )
                )
        );

        assertThat(result.compatibleItemCount()).isEqualTo(2);
        assertThat(result.factors().itemCombination().score())
                .isEqualByComparingTo("18.00");
    }

    @Test
    void distinctComplementaryCategoriesDetermineCategoryScore() {
        var result = scorer.score(
                List.of(PreferenceStyleTag.FORMAL),
                List.of(ItemCategory.LEATHER_GOODS),
                List.of(ColorGroup.BLUE),
                ItemCategory.BAG,
                ColorGroup.RED,
                List.of(),
                List.of("SPRING"),
                List.of(
                        candidate(
                                1L,
                                "재킷1",
                                ItemCategory.CLOTHING,
                                ColorGroup.BLUE
                        ),
                        candidate(
                                2L,
                                "재킷2",
                                ItemCategory.CLOTHING,
                                ColorGroup.BLUE
                        ),
                        candidate(
                                3L,
                                "슈즈",
                                ItemCategory.SHOES,
                                ColorGroup.BLUE
                        ),
                        candidate(
                                4L,
                                "가방",
                                ItemCategory.BAG,
                                ColorGroup.BLUE
                        )
                )
        );

        assertThat(result.factors()
                .categoryCombination()
                .complementaryCategoryCount())
                .isEqualTo(2);
        assertThat(result.factors()
                .categoryCombination()
                .score())
                .isEqualByComparingTo("14.00");
    }

    @Test
    void seasonBreadthUsesConfirmedScale() {
        var oneSeason = scoreSeason(List.of("SPRING"));
        var twoSeasons = scoreSeason(
                List.of("SPRING", "AUTUMN")
        );
        var threeSeasons = scoreSeason(
                List.of("SPRING", "SUMMER", "AUTUMN")
        );
        var fourSeasons = scoreSeason(
                List.of(
                        "SPRING",
                        "SUMMER",
                        "AUTUMN",
                        "WINTER"
                )
        );
        var allSeason = scoreSeason(
                List.of("ALL_SEASON")
        );

        assertThat(oneSeason.factors().season().score())
                .isEqualByComparingTo("10.00");
        assertThat(twoSeasons.factors().season().score())
                .isEqualByComparingTo("15.00");
        assertThat(threeSeasons.factors().season().score())
                .isEqualByComparingTo("20.00");
        assertThat(fourSeasons.factors().season().score())
                .isEqualByComparingTo("25.00");
        assertThat(allSeason.factors().season().score())
                .isEqualByComparingTo("25.00");
        assertThat(allSeason.factors().season().seasonCount())
                .isEqualTo(4);
        assertThat(allSeason.factors().season().allSeason())
                .isTrue();
    }

    private PurchaseUtilityScorer.PurchaseUtilityScoreResult scoreSeason(
            List<String> seasons
    ) {
        return scorer.score(
                List.of(PreferenceStyleTag.FORMAL),
                List.of(ItemCategory.LEATHER_GOODS),
                List.of(ColorGroup.BLUE),
                ItemCategory.BAG,
                ColorGroup.RED,
                List.of(),
                seasons,
                List.of()
        );
    }

    private PurchaseUtilityScorer.UserItemCandidate candidate(
            Long id,
            String name,
            ItemCategory category,
            ColorGroup color
    ) {
        return new PurchaseUtilityScorer.UserItemCandidate(
                id,
                name,
                category,
                color,
                null
        );
    }
}
