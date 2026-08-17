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
                "149b287bf4f9ef3d8a7c1c46a9edf3355b72a26211151c7c7de94c90570f756a"
        );
        assertThat(second).isEqualTo(first);
        assertThat(first).hasSize(64);
    }
}
