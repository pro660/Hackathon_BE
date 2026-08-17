package org.likelionhsu.hackathon.place.dto;

import java.math.BigDecimal;
import java.time.Instant;

import org.likelionhsu.hackathon.place.domain.PlaceCategory;

public record SavedPlaceResponse(
        String placeId,
        String name,
        PlaceCategory category,
        String categoryName,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl,
        boolean saved,
        Instant savedAt
) {
}
