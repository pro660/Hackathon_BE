package org.likelionhsu.hackathon.purchaseutility.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;
import org.mockito.Mockito;

class PurchaseUtilityAnalysisTest {

    @Test
    void applyingAiExplanationChangesOnlyExplanationState() {
        PurchaseUtilityAnalysis analysis = analysis();

        analysis.applyAiExplanation(
                "  보유 아이템과 여러 계절에 활용하기 좋은 제품입니다.  "
        );

        assertThat(analysis.getSummary())
                .isEqualTo(
                        "보유 아이템과 여러 계절에 활용하기 좋은 제품입니다."
                );
        assertThat(
                analysis
                        .getFactorJson()
                        .explanationGenerationType()
        ).isEqualTo(
                PurchaseUtilityExplanationGenerationType.AI
        );
        assertThat(analysis.getUtilityScore())
                .isEqualByComparingTo("77.00");
        assertThat(analysis.getCompatibleItemCount())
                .isEqualTo(2);
    }

    @Test
    void blankOrOversizedAiSummaryIsRejected() {
        PurchaseUtilityAnalysis analysis = analysis();

        assertThatThrownBy(() ->
                analysis.applyAiExplanation("   ")
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                analysis.applyAiExplanation(
                        "가".repeat(1501)
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThat(
                analysis
                        .getFactorJson()
                        .explanationGenerationType()
        ).isEqualTo(
                PurchaseUtilityExplanationGenerationType.RULE_BASED
        );
    }

    private PurchaseUtilityAnalysis analysis() {
        User user = Mockito.mock(User.class);
        Product product = Mockito.mock(Product.class);

        when(product.getId()).thenReturn(101L);
        when(product.getName()).thenReturn("Aren Shopper");
        when(product.getCategory())
                .thenReturn(ItemCategory.BAG);
        when(product.getPrimaryColor())
                .thenReturn(ColorGroup.BROWN);

        PurchaseUtilityFactorSnapshot factors =
                new PurchaseUtilityFactorSnapshot(
                        "purchase-utility-rule-v1",
                        PurchaseUtilityExplanationGenerationType.RULE_BASED,
                        new PurchaseUtilityFactorSnapshot.PreferenceFactor(
                                new BigDecimal("20.00"),
                                new BigDecimal("30.00"),
                                true,
                                true,
                                false
                        ),
                        new PurchaseUtilityFactorSnapshot.ItemCombinationFactor(
                                new BigDecimal("18.00"),
                                new BigDecimal("25.00"),
                                2,
                                List.of()
                        ),
                        new PurchaseUtilityFactorSnapshot.SeasonFactor(
                                new BigDecimal("25.00"),
                                new BigDecimal("25.00"),
                                4,
                                true
                        ),
                        new PurchaseUtilityFactorSnapshot
                                .CategoryCombinationFactor(
                                new BigDecimal("14.00"),
                                new BigDecimal("20.00"),
                                2
                        )
                );

        return PurchaseUtilityAnalysis.createRuleBased(
                user,
                product,
                new BigDecimal("77.00"),
                2,
                factors,
                "규칙 기반 분석 결과입니다.",
                9001L,
                Instant.parse("2026-08-16T00:00:00Z")
        );
    }
}
