package org.likelionhsu.hackathon.styleplan.dto;

import java.util.List;

public record StylePlanPreview(
        String previewId,
        String title,
        String description,
        List<OwnedItem> ownedItems,
        List<RecommendedProduct> recommendedProducts,
        String generationType
) {

    public StylePlanPreview {
        ownedItems = List.copyOf(ownedItems);
        recommendedProducts =
                List.copyOf(recommendedProducts);
    }

    public record OwnedItem(
            String myItemId,
            String name,
            String imageUrl,
            String role,
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
