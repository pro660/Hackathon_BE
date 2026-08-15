package org.likelionhsu.hackathon.recommendation.dto.response;

import java.math.BigDecimal;

public record RecommendationScoreBreakdownResponse(
        BigDecimal style,
        BigDecimal occasion,
        BigDecimal season,
        BigDecimal feature
) {
}
