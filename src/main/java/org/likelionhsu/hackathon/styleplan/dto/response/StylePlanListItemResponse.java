package org.likelionhsu.hackathon.styleplan.dto.response;

import java.time.Instant;

import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;

public record StylePlanListItemResponse(
        String stylePlanId,
        String title,
        StylePlanOccasion occasion,
        Instant plannedAt,
        StylePlanStatus status,
        String thumbnailImageUrl,
        int ownedItemCount,
        int recommendedProductCount,
        Instant createdAt
) {
}
