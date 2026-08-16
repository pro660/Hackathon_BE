package org.likelionhsu.hackathon.purchaseutility.domain;

import org.likelionhsu.hackathon.common.enums.MaterialGroup;

public final class CareDifficultyResolver {

    private CareDifficultyResolver() {
    }

    public static CareDifficulty resolve(MaterialGroup material) {
        if (material == null) {
            return CareDifficulty.UNKNOWN;
        }

        return switch (material) {
            case NYLON -> CareDifficulty.EASY;
            case SYNTHETIC_LEATHER,
                    CANVAS,
                    FABRIC,
                    METAL -> CareDifficulty.MODERATE;
            case LEATHER -> CareDifficulty.HARD;
            case OTHER, UNKNOWN -> CareDifficulty.UNKNOWN;
        };
    }
}
