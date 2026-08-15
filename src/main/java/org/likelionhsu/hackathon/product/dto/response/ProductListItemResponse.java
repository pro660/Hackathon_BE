package org.likelionhsu.hackathon.product.dto.response;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.product.entity.ProductBrand;

public record ProductListItemResponse(
        String productId,
        ProductBrand brand,
        String name,
        ItemCategory category,
        long price,
        ColorGroup primaryColor,
        String primaryImageUrl
) {
}