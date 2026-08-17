package org.likelionhsu.hackathon.styleplan.dto.response;

import java.time.Instant;
import java.util.List;

import org.likelionhsu.hackathon.styleplan.domain.StyleItemRole;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanWeatherCondition;

public record StylePlanDetailResponse(
        String stylePlanId,
        String title,
        StylePlanOccasion occasion,
        Instant plannedAt,
        StylePlanWeatherCondition weatherCondition,
        String description,
        StylePlanGenerationType generationType,
        StylePlanStatus status,
        List<OwnedItem> ownedItems,
        List<RecommendedProduct> recommendedProducts,
        List<Object> places,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    public StylePlanDetailResponse {
        ownedItems = List.copyOf(ownedItems);
        recommendedProducts = List.copyOf(recommendedProducts);
        places = List.copyOf(places);
    }

    public record OwnedItem(
            String myItemId,
            String name,
            String imageUrl,
            StyleItemRole role,
            int sortOrder
    ) {
    }

    public record RecommendedProduct(
            String productId,
            String name,
            String imageUrl,
            int rank,
            String reason
    ) {
    }
}
