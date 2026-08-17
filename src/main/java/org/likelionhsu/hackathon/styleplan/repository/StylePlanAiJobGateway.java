package org.likelionhsu.hackathon.styleplan.repository;

import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StylePlanAiJobGateway {

    private static final String JOB_TYPE = "STYLE_PLAN";

    private final JdbcTemplate jdbcTemplate;

    public StylePlanAiJobGateway(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean claimProcessing(
            Long userId,
            Long jobId
    ) {
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
        Objects.requireNonNull(inputHash);

        if (!inputHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "inputHash는 64자리 소문자 SHA-256이어야 합니다."
            );
        }

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

    public java.util.Optional<String> findReusableResultJson(
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

        if (inputHash == null
                || !inputHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "inputHash는 64자리 소문자 SHA-256이어야 합니다."
            );
        }

        return jdbcTemplate.query(
                """
                SELECT JSON_SET(
                           cached.result_json,
                           '$.previewId',
                           CONCAT('job:', current_job.id)
                       ) AS result_json
                FROM ai_jobs current_job
                JOIN ai_jobs cached
                  ON cached.user_id = current_job.user_id
                 AND cached.type = current_job.type
                 AND cached.input_hash = ?
                 AND cached.prompt_version =
                     current_job.prompt_version
                 AND cached.model = current_job.model
                 AND cached.status = 'SUCCEEDED'
                 AND cached.id <> current_job.id
                 AND cached.completed_at >=
                     CURRENT_TIMESTAMP(6) - INTERVAL 24 HOUR
                WHERE current_job.id = ?
                  AND current_job.user_id = ?
                  AND current_job.type = ?
                  AND current_job.status = 'PROCESSING'
                ORDER BY cached.completed_at DESC,
                         cached.id DESC
                LIMIT 1
                """,
                (resultSet, rowNumber) ->
                        resultSet.getString("result_json"),
                inputHash,
                jobId,
                userId,
                JOB_TYPE
        ).stream().findFirst();
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
        requireJsonText(resultJson, "resultJson");
        requireMetrics(
                inputTokens,
                outputTokens,
                latencyMs,
                retryCount
        );

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

    public boolean markFailedWithFallback(
            Long userId,
            Long jobId,
            String fallbackJson,
            String errorCode
    ) {
        return markFailedWithFallback(
                userId,
                jobId,
                fallbackJson,
                errorCode,
                0,
                null
        );
    }

    public boolean markFailedWithFallback(
            Long userId,
            Long jobId,
            String fallbackJson,
            String errorCode,
            int retryCount,
            Long latencyMs
    ) {
        requireJsonText(
                fallbackJson,
                "fallbackJson"
        );

        if (errorCode == null
                || errorCode.isBlank()) {
            throw new IllegalArgumentException(
                    "errorCode는 비어 있을 수 없습니다."
            );
        }

        requireMetrics(
                null,
                null,
                latencyMs,
                retryCount
        );

        int updated = jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'FAILED',
                    result_json = NULL,
                    fallback_json = ?,
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
                fallbackJson,
                latencyMs,
                retryCount,
                errorCode,
                jobId,
                userId,
                JOB_TYPE
        );

        return updated == 1;
    }

    private void requireMetrics(
            Integer inputTokens,
            Integer outputTokens,
            Long latencyMs,
            int retryCount
    ) {
        if (inputTokens != null && inputTokens < 0) {
            throw new IllegalArgumentException(
                    "inputTokens는 0 이상이어야 합니다."
            );
        }

        if (outputTokens != null
                && outputTokens < 0) {
            throw new IllegalArgumentException(
                    "outputTokens는 0 이상이어야 합니다."
            );
        }

        if (latencyMs != null && latencyMs < 0L) {
            throw new IllegalArgumentException(
                    "latencyMs는 0 이상이어야 합니다."
            );
        }

        if (retryCount < 0 || retryCount > 1) {
            throw new IllegalArgumentException(
                    "retryCount는 0 또는 1이어야 합니다."
            );
        }
    }

    private void requireJsonText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + "는 비어 있을 수 없습니다."
            );
        }
    }
}
