package org.likelionhsu.hackathon.product.dto.response;

public record ProductImageResponse(
        String url,
        String altText,
        int sortOrder,
        boolean isPrimary
) {
}