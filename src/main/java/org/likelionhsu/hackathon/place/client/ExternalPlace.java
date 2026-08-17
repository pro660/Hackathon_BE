package org.likelionhsu.hackathon.place.client;

import java.math.BigDecimal;

import org.likelionhsu.hackathon.place.domain.PlaceCategory;

public record ExternalPlace(
        String providerPlaceId,
        String name,
        PlaceCategory category,
        String categoryName,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl
) {
}
