package org.likelionhsu.hackathon.wishlist.dto.response;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.product.entity.ProductBrand;

public record WishlistItemResponse(
        String productId,
        ProductBrand brand,
        String name,
        ItemCategory category,
        long price,
        ColorGroup primaryColor,
        String primaryImageUrl,
        boolean favorited
) {
}