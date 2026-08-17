package org.likelionhsu.hackathon.cart.dto.response;

import java.time.Instant;

import org.likelionhsu.hackathon.product.entity.ProductBrand;

public record CartItemResponse(
        String cartItemId,
        String productId,
        ProductBrand brand,
        String name,
        long price,
        String primaryImageUrl,
        String productUrl,
        Instant addedAt
) {
}