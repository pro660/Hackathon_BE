package org.likelionhsu.hackathon.aijob.repository;

import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiJobCreationPolicyRepository {

    private final JdbcTemplate jdbcTemplate;

    public AiJobCreationPolicyRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "jdbcTemplate는 null일 수 없습니다."
        );
    }

    public void lockUser(Long userId) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );

        Long lockedUserId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM users
                WHERE id = ?
                FOR UPDATE
                """,
                Long.class,
                userId
        );

        if (lockedUserId == null) {
            throw new IllegalStateException(
                    "AI Job 생성 대상 사용자를 찾을 수 없습니다."
            );
        }
    }

    public int expireStaleRunningJobs(Long userId) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );

        return jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'FAILED',
                    result_json = NULL,
                    error_code = 'AI_JOB_TIMEOUT',
                    completed_at = CURRENT_TIMESTAMP(6),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE user_id = ?
                  AND (
                        (
                            status = 'PENDING'
                            AND created_at <=
                                CURRENT_TIMESTAMP(6)
                                - INTERVAL 2 MINUTE
                        )
                        OR
                        (
                            status = 'PROCESSING'
                            AND started_at IS NOT NULL
                            AND started_at <=
                                CURRENT_TIMESTAMP(6)
                                - INTERVAL 2 MINUTE
                        )
                  )
                """,
                userId
        );
    }

    public boolean existsRunningJobExceptIdempotencyKey(
            Long userId,
            String idempotencyKey
    ) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );
        requireText(
                idempotencyKey,
                "idempotencyKey"
        );

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM ai_jobs
                WHERE user_id = ?
                  AND status IN ('PENDING', 'PROCESSING')
                  AND idempotency_key <> ?
                """,
                Integer.class,
                userId,
                idempotencyKey
        );

        return count != null && count > 0;
    }

    public int countCreatedInLastTwentyFourHoursExceptIdempotencyKey(
            Long userId,
            String idempotencyKey
    ) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );
        requireText(
                idempotencyKey,
                "idempotencyKey"
        );

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM ai_jobs
                WHERE user_id = ?
                  AND created_at >
                      CURRENT_TIMESTAMP(6) - INTERVAL 24 HOUR
                  AND idempotency_key <> ?
                """,
                Integer.class,
                userId,
                idempotencyKey
        );

        return count == null ? 0 : count;
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
}
