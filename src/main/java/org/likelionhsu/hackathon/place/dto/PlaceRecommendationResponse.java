package org.likelionhsu.hackathon.place.dto;

import java.math.BigDecimal;
import java.util.List;

import org.likelionhsu.hackathon.place.domain.PlaceCategory;

public record PlaceRecommendationResponse(
        String stylePlanId,
        String rankingPolicyVersion,
        List<RecommendedPlace> places
) {
    public PlaceRecommendationResponse {
        places = List.copyOf(places);
    }

    public record RecommendedPlace(
            int rank,
            double score,
            ScoreBreakdown scoreBreakdown,
            String reasonCode,
            Place place
    ) {
    }

    public record ScoreBreakdown(
            double categorySuitability,
            double distance
    ) {
    }

    public record Place(
            String placeId,
            String name,
            PlaceCategory category,
            String categoryName,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String placeUrl,
            boolean saved
    ) {
    }
}
