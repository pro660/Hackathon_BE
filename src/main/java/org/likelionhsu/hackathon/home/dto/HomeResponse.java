package org.likelionhsu.hackathon.home.dto;

import java.math.BigDecimal;
import java.util.List;

public record HomeResponse(
        UserSummary user,
        LatestStylePlan latestStylePlan,
        List<RecommendedProduct> recommendedProducts
) {
    public HomeResponse {
        recommendedProducts = List.copyOf(recommendedProducts);
    }

    public record UserSummary(
            String nickname,
            boolean preferenceCompleted,
            long myItemCount
    ) {}

    public record LatestStylePlan(
            String stylePlanId,
            String title,
            String thumbnailImageUrl
    ) {}

    public record RecommendedProduct(
            String productId,
            String name,
            BigDecimal matchScore,
            String primaryImageUrl
    ) {}
}
