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

    public boolean markFailedWithFallback(
            Long userId,
            Long jobId,
            String fallbackJson,
            String errorCode
    ) {
        Objects.requireNonNull(fallbackJson);
        Objects.requireNonNull(errorCode);

        int updated = jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'FAILED',
                    result_json = NULL,
                    fallback_json = ?,
                    input_tokens = NULL,
                    output_tokens = NULL,
                    latency_ms = NULL,
                    retry_count = 0,
                    error_code = ?,
                    completed_at = CURRENT_TIMESTAMP(6),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND user_id = ?
                  AND type = ?
                  AND status = 'PROCESSING'
                """,
                fallbackJson,
                errorCode,
                jobId,
                userId,
                JOB_TYPE
        );

        return updated == 1;
    }
}
