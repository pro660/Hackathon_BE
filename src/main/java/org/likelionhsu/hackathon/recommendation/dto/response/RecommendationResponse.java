package org.likelionhsu.hackathon.recommendation.dto.response;

import java.time.Instant;
import java.util.List;

import org.likelionhsu.hackathon.recommendation.entity.RecommendationGenerationType;

public record RecommendationResponse(
        String recommendationId,
        RecommendationGenerationType generationType,
        String scorePolicyVersion,
        String summary,
        List<RecommendationProductResponse> products,
        Instant generatedAt
) {
    public RecommendationResponse {
        products = List.copyOf(products);
    }
}
