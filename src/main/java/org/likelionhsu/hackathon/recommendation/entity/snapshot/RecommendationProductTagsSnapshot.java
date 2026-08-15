package org.likelionhsu.hackathon.recommendation.entity.snapshot;

import java.util.List;

public record RecommendationProductTagsSnapshot(
        List<String> styles,
        List<String> seasons,
        List<String> occasions,
        List<String> features
) {
    public RecommendationProductTagsSnapshot {
        styles = List.copyOf(styles);
        seasons = List.copyOf(seasons);
        occasions = List.copyOf(occasions);
        features = List.copyOf(features);
    }

    public static RecommendationProductTagsSnapshot empty() {
        return new RecommendationProductTagsSnapshot(
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
