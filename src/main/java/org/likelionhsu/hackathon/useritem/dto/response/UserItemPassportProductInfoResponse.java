package org.likelionhsu.hackathon.useritem.dto.response;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;

public record UserItemPassportProductInfoResponse(
        String linkedProductId,
        String brandName,
        String name,
        ItemCategory category,
        ColorGroup primaryColor,
        MaterialGroup material,
        String imageUrl,
        String sku,
        String productUrl
) {
}
