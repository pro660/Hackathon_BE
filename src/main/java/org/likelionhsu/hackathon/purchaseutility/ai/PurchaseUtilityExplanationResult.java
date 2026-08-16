package org.likelionhsu.hackathon.purchaseutility.ai;

import java.util.Objects;

public record PurchaseUtilityExplanationResult(
        String summary,
        Integer inputTokens,
        Integer outputTokens,
        Long latencyMs
) {

    public PurchaseUtilityExplanationResult {
        summary = Objects.requireNonNull(
                summary,
                "summary는 null일 수 없습니다."
        ).trim();

        if (summary.isEmpty()) {
            throw new IllegalArgumentException(
                    "summary는 비어 있을 수 없습니다."
            );
        }

        if (inputTokens != null && inputTokens < 0) {
            throw new IllegalArgumentException(
                    "inputTokens는 0 이상이어야 합니다."
            );
        }

        if (outputTokens != null && outputTokens < 0) {
            throw new IllegalArgumentException(
                    "outputTokens는 0 이상이어야 합니다."
            );
        }

        if (latencyMs != null && latencyMs < 0) {
            throw new IllegalArgumentException(
                    "latencyMs는 0 이상이어야 합니다."
            );
        }
    }
}
