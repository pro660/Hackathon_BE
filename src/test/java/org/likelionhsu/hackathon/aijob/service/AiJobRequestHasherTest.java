package org.likelionhsu.hackathon.aijob.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class AiJobRequestHasherTest {

    private final AiJobRequestHasher hasher =
            new AiJobRequestHasher();

    @Test
    void purchaseUtilityHashIsDeterministic() {
        assertThat(
                hasher.hashPurchaseUtility("123")
        ).isEqualTo(
                "994472b614b9bba113162cb8d912777027abf2cae05b58023f9bb6ebdb1ea9e0"
        );

        assertThat(
                hasher.hashPurchaseUtility("123")
        ).hasSize(64);
    }

    @Test
    void itemAnalysisHashIsDeterministic() {
        assertThat(
                hasher.hashItemAnalysis("51")
        ).isEqualTo(
                "2255d1747ca5bef1373b592dce426d463e231d1a227ff5d4d607e51b801c5d8a"
        );

        assertThat(
                hasher.hashItemAnalysis("51")
        ).hasSize(64);
    }

    @Test
    void stylePlanHashIsDeterministicAndStyleTagOrderIndependent() {
        String first = hasher.hashStylePlan(
                "DATE",
                List.of("NEAT", "GLAMOROUS"),
                null,
                true,
                "ko"
        );
        String second = hasher.hashStylePlan(
                "DATE",
                List.of("GLAMOROUS", "NEAT"),
                null,
                true,
                "ko"
        );

        assertThat(first).isEqualTo(
                "743e7155d0ecd90f0b8ce3814a47db82e9d4144ef8ed1e2dc01a38717e4f92f2"
        );
        assertThat(second).isEqualTo(first);
        assertThat(first).hasSize(64);
    }

    @Test
    void stylePlanHashChangesWhenSliderLevelChanges() {
        String casualNeat = hasher.hashStylePlan(
                "DATE",
                1,
                1,
                List.of("CASUAL", "NEAT"),
                null,
                true,
                "ko"
        );
        String formalGlamorous = hasher.hashStylePlan(
                "DATE",
                10,
                10,
                List.of("FORMAL", "GLAMOROUS"),
                null,
                true,
                "ko"
        );

        assertThat(casualNeat)
                .isNotEqualTo(formalGlamorous);
        assertThat(casualNeat).hasSize(64);
        assertThat(formalGlamorous).hasSize(64);
    }
}
