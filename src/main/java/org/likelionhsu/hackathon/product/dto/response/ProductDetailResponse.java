package org.likelionhsu.hackathon.product.dto.response;

import java.util.List;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.product.entity.ProductBrand;

public record ProductDetailResponse(
        String productId,
        ProductBrand brand,
        String sku,
        String name,
        ItemCategory category,
        String description,
        long price,
        ColorGroup primaryColor,
        MaterialGroup material,
        String productUrl,
        List<ProductImageResponse> images,
        ProductTagsResponse tags,
        boolean favorited,
        boolean inCart
) {
}