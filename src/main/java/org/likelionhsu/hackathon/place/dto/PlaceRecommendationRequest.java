package org.likelionhsu.hackathon.place.dto;

import java.math.BigDecimal;

import org.likelionhsu.hackathon.place.domain.PlaceCategory;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceRecommendationRequest(
        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        BigDecimal latitude,

        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        BigDecimal longitude,

        @Min(1)
        @Max(20_000)
        Integer radius,

        PlaceCategory category,

        @Size(max = 200)
        String query
) {
    public int effectiveRadius() {
        return radius == null ? 3_000 : radius;
    }
}
