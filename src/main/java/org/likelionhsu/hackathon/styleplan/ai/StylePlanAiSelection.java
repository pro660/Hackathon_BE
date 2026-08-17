package org.likelionhsu.hackathon.styleplan.ai;

import java.util.List;
import java.util.Objects;

public record StylePlanAiSelection(
        String title,
        String description,
        List<OwnedItemSelection> ownedItems,
        List<ProductSelection> recommendedProducts
) {

    public StylePlanAiSelection {
        title = Objects.requireNonNull(title, "title");
        description = Objects.requireNonNull(
                description,
                "description"
        );
        ownedItems = List.copyOf(ownedItems);
        recommendedProducts =
                List.copyOf(recommendedProducts);
    }

    public record OwnedItemSelection(
            String myItemId,
            String role
    ) {
    }

    public record ProductSelection(
            String productId,
            String reason
    ) {
    }
}
