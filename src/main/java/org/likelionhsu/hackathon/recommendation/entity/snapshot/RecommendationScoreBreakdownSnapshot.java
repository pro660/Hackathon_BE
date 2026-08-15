package org.likelionhsu.hackathon.recommendation.entity.snapshot;

import java.math.BigDecimal;

public record RecommendationScoreBreakdownSnapshot(
        BigDecimal style,
        BigDecimal occasion,
        BigDecimal season,
        BigDecimal feature
) {
}
