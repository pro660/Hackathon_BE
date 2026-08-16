package org.likelionhsu.hackathon.purchaseutility.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.purchaseutility.domain.CareDifficulty;

class PurchaseUtilityAiInputHasherTest {

    private final PurchaseUtilityAiInputHasher hasher =
            new PurchaseUtilityAiInputHasher();

    @Test
    void analysisIdDoesNotAffectInputHash() {
        String first = hasher.hash(
                request("801", "ko", new BigDecimal("77.00"))
        );
        String second = hasher.hash(
                request("999", "ko", new BigDecimal("77.0"))
        );

        assertThat(first)
                .matches("[0-9a-f]{64}")
                .isEqualTo(second);
    }

    @Test
    void changedAiInputChangesInputHash() {
        String korean = hasher.hash(
                request("801", "ko", new BigDecimal("77.00"))
        );
        String english = hasher.hash(
                request("801", "en", new BigDecimal("77.00"))
        );
        String changedScore = hasher.hash(
                request("801", "ko", new BigDecimal("78.00"))
        );

        assertThat(english).isNotEqualTo(korean);
        assertThat(changedScore).isNotEqualTo(korean);
    }

    private PurchaseUtilityExplanationRequest request(
            String analysisId,
            String language,
            BigDecimal utilityScore
    ) {
        return new PurchaseUtilityExplanationRequest(
                analysisId,
                "purchase-utility-rule-v1",
                new PurchaseUtilityExplanationRequest.ProductContext(
                        "101",
                        "Aren Shopper",
                        ItemCategory.BAG,
                        ColorGroup.BROWN
                ),
                utilityScore,
                new PurchaseUtilityExplanationRequest.FactorScores(
                        new BigDecimal("20.00"),
                        new BigDecimal("18.00"),
                        new BigDecimal("25.00"),
                        new BigDecimal("14.00")
                ),
                0,
                List.of(),
                CareDifficulty.MODERATE,
                language
        );
    }
}
