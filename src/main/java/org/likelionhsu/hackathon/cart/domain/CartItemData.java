package org.likelionhsu.hackathon.cart.domain;

import java.time.Instant;

import org.likelionhsu.hackathon.product.entity.ProductBrand;

public record CartItemData(
        Long cartItemId,
        Long productId,
        ProductBrand brand,
        String name,
        long price,
        String primaryImageUrl,
        String productUrl,
        Instant addedAt
) {
}