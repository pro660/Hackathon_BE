package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;

class StylePlanFallbackServiceTest {

    @Test
    void fallbackKeepsLimitsAndRanks() {
        StylePlanRecommendationContext context =
                new StylePlanRecommendationContext(
                        new StylePlanJobRequest(
                                "DATE",
                                List.of("NEAT"),
                                null,
                                true,
                                "ko"
                        ),
                        List.of("NEAT"),
                        List.of("BLACK"),
                        List.of("BAG"),
                        List.of(
                                owned("1", "BAG"),
                                owned("2", "LEATHER_GOODS"),
                                owned("3", "FASHION_ACCESSORY"),
                                owned("4", "CLOTHING"),
                                owned("5", "SHOES")
                        ),
                        List.of(
                                product("101", 9),
                                product("102", 8),
                                product("103", 7),
                                product("104", 6)
                        )
                );

        StylePlanPreview preview =
                new StylePlanFallbackService()
                        .build(9201L, context);

        assertThat(preview.previewId())
                .isEqualTo("job:9201");
        assertThat(preview.title())
                .isEqualTo("데이트 룩");
        assertThat(preview.ownedItems())
                .hasSize(5);
        assertThat(preview.ownedItems())
                .extracting(
                        StylePlanPreview.OwnedItem::role
                )
                .containsExactly(
                        "BAG",
                        "ACCESSORY",
                        "ACCESSORY",
                        "MAIN",
                        "SHOES"
                );
        assertThat(preview.ownedItems())
                .extracting(
                        StylePlanPreview.OwnedItem::sortOrder
                )
                .containsExactly(0, 1, 2, 3, 4);
        assertThat(preview.recommendedProducts())
                .hasSize(3);
        assertThat(preview.recommendedProducts())
                .extracting(
                        StylePlanPreview
                                .RecommendedProduct::rank
                )
                .containsExactly(1, 2, 3);
        assertThat(preview.generationType())
                .isEqualTo("RULE_BASED");
    }

    private StylePlanRecommendationContext.OwnedItemCandidate
            owned(
            String id,
            String category
    ) {
        return new StylePlanRecommendationContext
                .OwnedItemCandidate(
                id,
                "보유 아이템 " + id,
                null,
                category,
                null,
                null,
                0L,
                0
        );
    }

    private StylePlanRecommendationContext.ProductCandidate
            product(
            String id,
            int score
    ) {
        return new StylePlanRecommendationContext
                .ProductCandidate(
                id,
                "MCM 상품 " + id,
                null,
                "BAG",
                "BLACK",
                "LEATHER",
                List.of("DATE", "NEAT"),
                score
        );
    }
}
