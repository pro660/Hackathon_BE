package org.likelionhsu.hackathon.aijob.domain;

import java.time.Instant;

public record AiJobData(
        Long id,
        Long userId,
        AiJobType type,
        AiJobStatus status,
        String idempotencyKey,
        String requestHash,
        String model,
        String promptVersion,
        String inputHash,
        String resultJson,
        String fallbackJson,
        Integer inputTokens,
        Integer outputTokens,
        Long latencyMs,
        int retryCount,
        String errorCode,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
