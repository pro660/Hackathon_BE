package org.likelionhsu.hackathon.recommendation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.likelionhsu.hackathon.recommendation.entity.RecommendationFeature;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationOccasion;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationSeason;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationProductTagsSnapshot;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationScoreBreakdownSnapshot;
import org.springframework.stereotype.Component;

@Component
public class RecommendationScorer {

    private static final BigDecimal STYLE_SCORE =
            new BigDecimal("30.00");
    private static final BigDecimal OCCASION_SCORE =
            new BigDecimal("25.00");
    private static final BigDecimal SEASON_SCORE =
            new BigDecimal("25.00");
    private static final BigDecimal FEATURE_MAX_SCORE =
            new BigDecimal("20.00");

    public RecommendationScoreResult score(
            List<String> preferredStyleTags,
            RecommendationOccasion occasion,
            RecommendationSeason season,
            List<RecommendationFeature> preferredFeatures,
            RecommendationProductTagsSnapshot productTags
    ) {
        Set<String> productStyles =
                Set.copyOf(productTags.styles());
        Set<String> productOccasions =
                Set.copyOf(productTags.occasions());
        Set<String> productSeasons =
                Set.copyOf(productTags.seasons());
        Set<String> productFeatures =
                Set.copyOf(productTags.features());

        boolean styleMatched =
                preferredStyleTags
                        .stream()
                        .anyMatch(productStyles::contains);

        boolean occasionMatched =
                productOccasions.contains(occasion.name());

        boolean directSeasonMatched =
                productSeasons.contains(season.name());
        boolean allSeasonMatched =
                productSeasons.contains("ALL_SEASON");
        boolean seasonMatched =
                directSeasonMatched || allSeasonMatched;

        List<RecommendationFeature> matchedFeatures =
                preferredFeatures
                        .stream()
                        .filter(feature ->
                                productFeatures.contains(
                                        feature.name()
                                )
                        )
                        .toList();

        BigDecimal styleScore =
                styleMatched
                        ? STYLE_SCORE
                        : zero();
        BigDecimal occasionScore =
                occasionMatched
                        ? OCCASION_SCORE
                        : zero();
        BigDecimal seasonScore =
                seasonMatched
                        ? SEASON_SCORE
                        : zero();
        BigDecimal featureScore =
                calculateFeatureScore(
                        matchedFeatures.size(),
                        preferredFeatures.size()
                );

        RecommendationScoreBreakdownSnapshot breakdown =
                new RecommendationScoreBreakdownSnapshot(
                        styleScore,
                        occasionScore,
                        seasonScore,
                        featureScore
                );

        BigDecimal total =
                styleScore
                        .add(occasionScore)
                        .add(seasonScore)
                        .add(featureScore)
                        .setScale(2, RoundingMode.HALF_UP);

        return new RecommendationScoreResult(
                total,
                breakdown,
                allSeasonMatched && !directSeasonMatched,
                matchedFeatures
        );
    }

    private BigDecimal calculateFeatureScore(
            int matchedCount,
            int requestedCount
    ) {
        if (matchedCount == 0) {
            return zero();
        }

        return FEATURE_MAX_SCORE
                .multiply(BigDecimal.valueOf(matchedCount))
                .divide(
                        BigDecimal.valueOf(requestedCount),
                        10,
                        RoundingMode.HALF_UP
                )
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero() {
        return new BigDecimal("0.00");
    }

    public record RecommendationScoreResult(
            BigDecimal total,
            RecommendationScoreBreakdownSnapshot breakdown,
            boolean allSeasonOnlyMatch,
            List<RecommendationFeature> matchedFeatures
    ) {
        public RecommendationScoreResult {
            matchedFeatures = List.copyOf(matchedFeatures);
        }

        public boolean hasScore() {
            return total.compareTo(BigDecimal.ZERO) > 0;
        }
    }
}
