package org.likelionhsu.hackathon.itemanalysis.ai;

import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ItemAnalysisAiJobGateway {

    private static final String JOB_TYPE = "ITEM_ANALYSIS";
    private static final int SHA_256_HEX_LENGTH = 64;

    private final JdbcTemplate jdbcTemplate;

    public ItemAnalysisAiJobGateway(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean claimProcessing(
            Long userId,
            Long jobId
    ) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                jobId,
                "jobId는 null일 수 없습니다."
        );

        int updated = jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'PROCESSING',
                    started_at = CURRENT_TIMESTAMP(6),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND user_id = ?
                  AND type = ?
                  AND status = 'PENDING'
                """,
                jobId,
                userId,
                JOB_TYPE
        );

        return updated == 1;
    }

    public boolean updateInputHashIfProcessing(
            Long userId,
            Long jobId,
            String inputHash
    ) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                jobId,
                "jobId는 null일 수 없습니다."
        );
        requireSha256Hash(
                inputHash,
                "inputHash"
        );

        int updated = jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET input_hash = ?,
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND user_id = ?
                  AND type = ?
                  AND status = 'PROCESSING'
                """,
                inputHash,
                jobId,
                userId,
                JOB_TYPE
        );

        return updated == 1;
    }

    public boolean markSucceeded(
            Long userId,
            Long jobId,
            String resultJson,
            Integer inputTokens,
            Integer outputTokens,
            Long latencyMs,
            int retryCount
    ) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                jobId,
                "jobId는 null일 수 없습니다."
        );
        requireText(
                resultJson,
                "resultJson"
        );
        requireNonNegative(
                inputTokens,
                "inputTokens"
        );
        requireNonNegative(
                outputTokens,
                "outputTokens"
        );
        requireNonNegative(
                latencyMs,
                "latencyMs"
        );
        requireRetryCount(retryCount);

        int updated = jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'SUCCEEDED',
                    result_json = ?,
                    fallback_json = NULL,
                    input_tokens = ?,
                    output_tokens = ?,
                    latency_ms = ?,
                    retry_count = ?,
                    error_code = NULL,
                    completed_at = CURRENT_TIMESTAMP(6),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND user_id = ?
                  AND type = ?
                  AND status = 'PROCESSING'
                """,
                resultJson,
                inputTokens,
                outputTokens,
                latencyMs,
                retryCount,
                jobId,
                userId,
                JOB_TYPE
        );

        return updated == 1;
    }

    public boolean markFailed(
            Long userId,
            Long jobId,
            String errorCode,
            Long latencyMs,
            int retryCount
    ) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                jobId,
                "jobId는 null일 수 없습니다."
        );
        requireText(
                errorCode,
                "errorCode"
        );
        requireNonNegative(
                latencyMs,
                "latencyMs"
        );
        requireRetryCount(retryCount);

        int updated = jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'FAILED',
                    result_json = NULL,
                    fallback_json = NULL,
                    input_tokens = NULL,
                    output_tokens = NULL,
                    latency_ms = ?,
                    retry_count = ?,
                    error_code = ?,
                    completed_at = CURRENT_TIMESTAMP(6),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND user_id = ?
                  AND type = ?
                  AND status = 'PROCESSING'
                """,
                latencyMs,
                retryCount,
                errorCode,
                jobId,
                userId,
                JOB_TYPE
        );

        return updated == 1;
    }

    private void requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + "는 비어 있을 수 없습니다."
            );
        }
    }

    private void requireSha256Hash(
            String value,
            String field
    ) {
        requireText(value, field);

        if (value.length() != SHA_256_HEX_LENGTH
                || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    field
                            + "는 64자리 소문자 SHA-256이어야 합니다."
            );
        }
    }

    private void requireRetryCount(
            int retryCount
    ) {
        if (retryCount < 0 || retryCount > 1) {
            throw new IllegalArgumentException(
                    "retryCount는 0 또는 1이어야 합니다."
            );
        }
    }

    private void requireNonNegative(
            Number value,
            String field
    ) {
        if (value != null
                && value.longValue() < 0L) {
            throw new IllegalArgumentException(
                    field + "는 0 이상이어야 합니다."
            );
        }
    }
}
