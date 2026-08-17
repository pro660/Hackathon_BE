package org.likelionhsu.hackathon.place.client;

import java.math.BigDecimal;

import org.likelionhsu.hackathon.place.domain.PlaceCategory;

public record PlaceSearchCommand(
        String query,
        PlaceCategory category,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radius
) {
}
