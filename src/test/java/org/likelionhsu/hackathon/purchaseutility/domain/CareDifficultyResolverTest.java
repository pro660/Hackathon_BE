package org.likelionhsu.hackathon.purchaseutility.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;

class CareDifficultyResolverTest {

    @Test
    void resolvesMaterialGroupToCareDifficulty() {
        assertThat(
                CareDifficultyResolver.resolve(MaterialGroup.NYLON)
        ).isEqualTo(CareDifficulty.EASY);

        assertThat(
                CareDifficultyResolver.resolve(
                        MaterialGroup.SYNTHETIC_LEATHER
                )
        ).isEqualTo(CareDifficulty.MODERATE);
        assertThat(
                CareDifficultyResolver.resolve(MaterialGroup.CANVAS)
        ).isEqualTo(CareDifficulty.MODERATE);
        assertThat(
                CareDifficultyResolver.resolve(MaterialGroup.FABRIC)
        ).isEqualTo(CareDifficulty.MODERATE);
        assertThat(
                CareDifficultyResolver.resolve(MaterialGroup.METAL)
        ).isEqualTo(CareDifficulty.MODERATE);

        assertThat(
                CareDifficultyResolver.resolve(MaterialGroup.LEATHER)
        ).isEqualTo(CareDifficulty.HARD);

        assertThat(
                CareDifficultyResolver.resolve(MaterialGroup.OTHER)
        ).isEqualTo(CareDifficulty.UNKNOWN);
        assertThat(
                CareDifficultyResolver.resolve(MaterialGroup.UNKNOWN)
        ).isEqualTo(CareDifficulty.UNKNOWN);
        assertThat(
                CareDifficultyResolver.resolve(null)
        ).isEqualTo(CareDifficulty.UNKNOWN);
    }
}
