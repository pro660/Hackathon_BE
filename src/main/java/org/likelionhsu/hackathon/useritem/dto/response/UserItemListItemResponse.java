package org.likelionhsu.hackathon.useritem.dto.response;

import java.time.Instant;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;

public record UserItemListItemResponse(
        String myItemId,
        String name,
        String brandName,
        ItemCategory category,
        ColorGroup primaryColor,
        MaterialGroup material,
        String primaryImageUrl,
        Instant createdAt
) {
}
