package org.likelionhsu.hackathon.aijob.service;

import static org.assertj.core.api.Assertions.assertThat;

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
}
