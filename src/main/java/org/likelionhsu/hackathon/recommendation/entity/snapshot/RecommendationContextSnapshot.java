package org.likelionhsu.hackathon.recommendation.entity.snapshot;

import java.util.List;

import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationFeature;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationOccasion;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationSeason;

public record RecommendationContextSnapshot(
        String scorePolicyVersion,
        List<String> preferredStyleTags,
        RecommendationOccasion occasion,
        RecommendationSeason season,
        List<RecommendationFeature> preferredFeatures,
        ItemCategory category,
        int limit
) {
    public RecommendationContextSnapshot {
        preferredStyleTags = List.copyOf(preferredStyleTags);
        preferredFeatures = List.copyOf(preferredFeatures);
    }
}
