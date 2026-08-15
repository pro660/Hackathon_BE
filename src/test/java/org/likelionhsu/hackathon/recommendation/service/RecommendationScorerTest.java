package org.likelionhsu.hackathon.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationFeature;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationOccasion;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationSeason;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationProductTagsSnapshot;

class RecommendationScorerTest {

    private final RecommendationScorer scorer =
            new RecommendationScorer();

    @Test
    void fullMatchScoresOneHundred() {
        RecommendationProductTagsSnapshot tags =
                new RecommendationProductTagsSnapshot(
                        List.of("CASUAL"),
                        List.of("AUTUMN"),
                        List.of("DATE"),
                        List.of("COMPACT", "MULTIWAY")
                );

        var result = scorer.score(
                List.of("CASUAL"),
                RecommendationOccasion.DATE,
                RecommendationSeason.AUTUMN,
                List.of(
                        RecommendationFeature.COMPACT,
                        RecommendationFeature.MULTIWAY
                ),
                tags
        );

        assertThat(result.total())
                .isEqualByComparingTo("100.00");
        assertThat(result.breakdown().style())
                .isEqualByComparingTo("30.00");
        assertThat(result.breakdown().occasion())
                .isEqualByComparingTo("25.00");
        assertThat(result.breakdown().season())
                .isEqualByComparingTo("25.00");
        assertThat(result.breakdown().feature())
                .isEqualByComparingTo("20.00");
    }

    @Test
    void partialFeatureMatchRoundsHalfUpToTwoDecimals() {
        RecommendationProductTagsSnapshot tags =
                new RecommendationProductTagsSnapshot(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("COMPACT")
                );

        var result = scorer.score(
                List.of("FORMAL"),
                RecommendationOccasion.DATE,
                RecommendationSeason.WINTER,
                List.of(
                        RecommendationFeature.COMPACT,
                        RecommendationFeature.MULTIWAY,
                        RecommendationFeature.SPACIOUS
                ),
                tags
        );

        assertThat(result.total())
                .isEqualByComparingTo("6.67");
        assertThat(result.breakdown().feature())
                .isEqualByComparingTo("6.67");
    }

    @Test
    void allSeasonTagReceivesSeasonScore() {
        RecommendationProductTagsSnapshot tags =
                new RecommendationProductTagsSnapshot(
                        List.of(),
                        List.of("ALL_SEASON"),
                        List.of(),
                        List.of()
                );

        var result = scorer.score(
                List.of("FORMAL"),
                RecommendationOccasion.DATE,
                RecommendationSeason.WINTER,
                List.of(RecommendationFeature.COMPACT),
                tags
        );

        assertThat(result.total())
                .isEqualByComparingTo("25.00");
        assertThat(result.allSeasonOnlyMatch()).isTrue();
    }

    @Test
    void noMatchScoresZero() {
        RecommendationProductTagsSnapshot tags =
                RecommendationProductTagsSnapshot.empty();

        var result = scorer.score(
                List.of("CASUAL"),
                RecommendationOccasion.DATE,
                RecommendationSeason.AUTUMN,
                List.of(RecommendationFeature.COMPACT),
                tags
        );

        assertThat(result.total())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.hasScore()).isFalse();
    }
}
