package org.likelionhsu.hackathon.purchaseutility.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.purchaseutility.domain.CareDifficulty;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityExplanationGenerationType;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityCompatibleItemSnapshot;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;

class PurchaseUtilityExplanationRequestTest {

    @Test
    void createsStructuredAiInputWithoutChangingRuleScore() throws Exception {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(101L);
        when(product.getName()).thenReturn("Aren Shopper");
        when(product.getCategory())
                .thenReturn(ItemCategory.BAG);
        when(product.getPrimaryColor())
                .thenReturn(ColorGroup.BROWN);
        when(product.getMaterial())
                .thenReturn(MaterialGroup.LEATHER);

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
                                1,
                                List.of(
                                        new PurchaseUtilityCompatibleItemSnapshot(
                                                "501",
                                                "베이지 재킷",
                                                ItemCategory.CLOTHING,
                                                ColorGroup.BEIGE,
                                                "https://example.com/item.webp",
                                                "구매 후보 제품과 색상 조합이 가능한 아이템입니다."
                                        )
                                )
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

        PurchaseUtilityAnalysis analysis =
                PurchaseUtilityAnalysis.createRuleBased(
                        mock(User.class),
                        product,
                        new BigDecimal("77.00"),
                        1,
                        factors,
                        "규칙 기반 분석 결과입니다.",
                        9001L,
                        Instant.parse("2026-08-16T00:00:00Z")
                );

        setId(analysis, 801L);

        PurchaseUtilityExplanationRequest request =
                PurchaseUtilityExplanationRequest.from(
                        analysis,
                        " ko "
                );

        assertThat(request.analysisId()).isEqualTo("801");
        assertThat(request.scorePolicyVersion())
                .isEqualTo("purchase-utility-rule-v1");
        assertThat(request.product().productId())
                .isEqualTo("101");
        assertThat(request.utilityScore())
                .isEqualByComparingTo("77.00");
        assertThat(request.factors().styleCombinationScore())
                .isEqualByComparingTo("18.00");
        assertThat(request.compatibleItems())
                .hasSize(1);
        assertThat(request.compatibleItems().getFirst().myItemId())
                .isEqualTo("501");
        assertThat(request.careDifficulty())
                .isEqualTo(CareDifficulty.HARD);
        assertThat(request.language()).isEqualTo("ko");
        assertThat(
                analysis
                        .getFactorJson()
                        .explanationGenerationType()
        ).isEqualTo(
                PurchaseUtilityExplanationGenerationType.RULE_BASED
        );
    }

    private void setId(
            PurchaseUtilityAnalysis analysis,
            Long id
    ) throws Exception {
        Field idField =
                PurchaseUtilityAnalysis.class
                        .getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(analysis, id);
    }
}
