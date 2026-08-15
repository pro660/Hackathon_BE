package org.likelionhsu.hackathon.recommendation.dto.response;

import java.math.BigDecimal;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.product.dto.response.ProductTagsResponse;

public record RecommendationProductResponse(
        String productId,
        String name,
        ItemCategory category,
        long price,
        ColorGroup primaryColor,
        String primaryImageUrl,
        ProductTagsResponse tags,
        BigDecimal score,
        RecommendationScoreBreakdownResponse scoreBreakdown,
        String reason,
        boolean favorited
) {
}
