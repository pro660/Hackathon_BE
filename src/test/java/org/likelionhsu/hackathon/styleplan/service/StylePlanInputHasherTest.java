package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class StylePlanInputHasherTest {

    private final StylePlanInputHasher hasher =
            new StylePlanInputHasher();

    @Test
    void sameContextHasSameSha256Hash() {
        StylePlanRecommendationContext context =
                new StylePlanRecommendationContext(
                        new StylePlanJobRequest(
                                "DATE",
                                List.of("NEAT"),
                                null,
                                true,
                                "ko"
                        ),
                        List.of("NEAT"),
                        List.of("BLACK"),
                        List.of("BAG"),
                        List.of(
                                new StylePlanRecommendationContext
                                        .OwnedItemCandidate(
                                        "1",
                                        "가방",
                                        null,
                                        "BAG",
                                        "BLACK",
                                        "LEATHER",
                                        2L,
                                        5
                                )
                        ),
                        List.of(
                                new StylePlanRecommendationContext
                                        .ProductCandidate(
                                        "101",
                                        "MCM 가방",
                                        null,
                                        "BAG",
                                        "BLACK",
                                        "LEATHER",
                                        List.of("DATE", "NEAT"),
                                        11
                                )
                        )
                );

        String first = hasher.hash(context);
        String second = hasher.hash(context);

        assertThat(first).isEqualTo(second);
        assertThat(first).matches("[0-9a-f]{64}");
    }
}
