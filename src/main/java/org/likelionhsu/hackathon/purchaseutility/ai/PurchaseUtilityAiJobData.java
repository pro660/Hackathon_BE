package org.likelionhsu.hackathon.purchaseutility.ai;

import java.time.Instant;

public record PurchaseUtilityAiJobData(
        Long id,
        Long userId,
        PurchaseUtilityAiJobStatus status,
        String idempotencyKey,
        String model,
        String promptVersion,
        String inputHash,
        String resultJson,
        String fallbackJson,
        int retryCount,
        String errorCode,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
