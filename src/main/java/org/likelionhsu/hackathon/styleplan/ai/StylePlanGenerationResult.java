package org.likelionhsu.hackathon.styleplan.ai;

import java.util.Objects;

public record StylePlanGenerationResult(
        StylePlanAiSelection selection,
        Integer inputTokens,
        Integer outputTokens,
        Long latencyMs
) {

    public StylePlanGenerationResult {
        Objects.requireNonNull(
                selection,
                "selection은 null일 수 없습니다."
        );

        requireNonNegative(inputTokens, "inputTokens");
        requireNonNegative(outputTokens, "outputTokens");

        if (latencyMs != null && latencyMs < 0L) {
            throw new IllegalArgumentException(
                    "latencyMs는 0 이상이어야 합니다."
            );
        }
    }

    private static void requireNonNegative(
            Integer value,
            String field
    ) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(
                    field + "는 0 이상이어야 합니다."
            );
        }
    }
}
