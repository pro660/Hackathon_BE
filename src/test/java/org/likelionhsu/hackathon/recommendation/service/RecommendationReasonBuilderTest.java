package org.likelionhsu.hackathon.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationFeature;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationOccasion;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationSeason;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationProductTagsSnapshot;

class RecommendationReasonBuilderTest {

    private final RecommendationScorer scorer =
            new RecommendationScorer();
    private final RecommendationReasonBuilder reasonBuilder =
            new RecommendationReasonBuilder();

    @Test
    void reasonContainsOnlyMatchedFactors() {
        var score = scorer.score(
                List.of("CASUAL"),
                RecommendationOccasion.DATE,
                RecommendationSeason.AUTUMN,
                List.of(
                        RecommendationFeature.COMPACT,
                        RecommendationFeature.MULTIWAY
                ),
                new RecommendationProductTagsSnapshot(
                        List.of("CASUAL"),
                        List.of(),
                        List.of("DATE"),
                        List.of("COMPACT")
                )
        );

        String reason = reasonBuilder.buildReason(
                score,
                RecommendationOccasion.DATE,
                RecommendationSeason.AUTUMN
        );

        assertThat(reason)
                .contains("선호 스타일")
                .contains("데이트 상황")
                .contains("컴팩트함")
                .doesNotContain("가을 시즌")
                .doesNotContain("다양한 연출");
    }
}
