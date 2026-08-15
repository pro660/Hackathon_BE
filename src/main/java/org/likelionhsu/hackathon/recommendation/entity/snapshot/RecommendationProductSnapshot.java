package org.likelionhsu.hackathon.recommendation.entity.snapshot;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;

public record RecommendationProductSnapshot(
        String productId,
        String name,
        ItemCategory category,
        long price,
        ColorGroup primaryColor,
        String primaryImageUrl,
        RecommendationProductTagsSnapshot tags,
        RecommendationScoreBreakdownSnapshot scoreBreakdown
) {
}
