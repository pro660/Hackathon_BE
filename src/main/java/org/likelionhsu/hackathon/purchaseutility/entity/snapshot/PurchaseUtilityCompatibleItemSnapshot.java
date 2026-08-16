package org.likelionhsu.hackathon.purchaseutility.entity.snapshot;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;

public record PurchaseUtilityCompatibleItemSnapshot(
        String myItemId,
        String name,
        ItemCategory category,
        ColorGroup primaryColor,
        String imageUrl,
        String reason
) {
}
