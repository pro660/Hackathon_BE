package org.likelionhsu.hackathon.purchaseutility.ai;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PurchaseUtilityAiJobGateway {

    private static final String JOB_TYPE =
            "PURCHASE_UTILITY";

    private final JdbcTemplate jdbcTemplate;

    public PurchaseUtilityAiJobGateway(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createPending(
            Long userId,
            String idempotencyKey,
            String model,
            String promptVersion,
            String inputHash
    ) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        requireText(idempotencyKey, "idempotencyKey");
        requireText(model, "model");
        requireText(promptVersion, "promptVersion");
        requireText(inputHash, "inputHash");

        KeyHolder keyHolder =
                new GeneratedKeyHolder();

        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    """
                                    INSERT INTO ai_jobs (
                                        user_id,
                                        type,
                                        status,
                                        idempotency_key,
                                        model,
                                        prompt_version,
                                        input_hash,
                                        retry_count,
                                        created_at,
                                        updated_at
                                    )
                                    VALUES (
                                        ?,
                                        'PURCHASE_UTILITY',
                                        'PENDING',
                                        ?,
                                        ?,
                                        ?,
                                        ?,
                                        0,
                                        CURRENT_TIMESTAMP(6),
                                        CURRENT_TIMESTAMP(6)
                                    )
                                    """,
                                    Statement.RETURN_GENERATED_KEYS
                            );

                    statement.setLong(1, userId);
                    statement.setString(2, idempotencyKey);
                    statement.setString(3, model);
                    statement.setString(4, promptVersion);
                    statement.setString(5, inputHash);

                    return statement;
                },
                keyHolder
        );

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException(
                    "생성된 AI Job ID를 가져오지 못했습니다."
            );
        }

        return key.longValue();
    }

    public Optional<PurchaseUtilityAiJobData>
    findByUserAndIdempotencyKey(
            Long userId,
            String idempotencyKey
    ) {
        requireText(idempotencyKey, "idempotencyKey");

        return jdbcTemplate.query(
                selectSql()
                        + """
                         WHERE user_id = ?
                           AND type = 'PURCHASE_UTILITY'
                           AND idempotency_key = ?
                         LIMIT 1
                        """,
                this::map,
                userId,
                idempotencyKey
        ).stream().findFirst();
    }

    public Optional<PurchaseUtilityAiJobData> findOwned(
            Long userId,
            Long jobId
    ) {
        return jdbcTemplate.query(
                selectSql()
                        + """
                         WHERE id = ?
                           AND user_id = ?
                           AND type = 'PURCHASE_UTILITY'
                         LIMIT 1
                        """,
                this::map,
                jobId,
                userId
        ).stream().findFirst();
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
                  AND type = 'PURCHASE_UTILITY'
                  AND status = 'PENDING'
                """,
                jobId,
                userId
        );

        return updated == 1;
    }

    public boolean updateInputHashIfProcessing(
            Long userId,
            Long jobId,
            String inputHash
    ) {
        requireText(inputHash, "inputHash");

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
                  AND type = 'PURCHASE_UTILITY'
                  AND status = 'PROCESSING'
                """,
                inputHash,
                jobId,
                userId
        );

        return updated == 1;
    }

    public boolean markSucceeded(
            Long userId,
            Long jobId,
            String resultJson,
            Integer inputTokens,
            Integer outputTokens,
            Long latencyMs
    ) {
        return markSucceeded(
                userId,
                jobId,
                resultJson,
                inputTokens,
                outputTokens,
                latencyMs,
                0
        );
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

        if (retryCount < 0 || retryCount > 1) {
            throw new IllegalArgumentException(
                    "retryCount는 0 또는 1이어야 합니다."
            );
        }

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
                  AND type = 'PURCHASE_UTILITY'
                  AND status = 'PROCESSING'
                """,
                resultJson,
                inputTokens,
                outputTokens,
                latencyMs,
                retryCount,
                jobId,
                userId
        );

        return updated == 1;
    }

    public boolean markFailed(
            Long userId,
            Long jobId,
            String fallbackJson,
            String errorCode,
            int retryCount,
            Long latencyMs
    ) {
        requireJsonText(fallbackJson, "fallbackJson");

        if (retryCount < 0 || retryCount > 1) {
            throw new IllegalArgumentException(
                    "retryCount는 0 또는 1이어야 합니다."
            );
        }

        int updated = jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'FAILED',
                    result_json = NULL,
                    fallback_json = ?,
                    latency_ms = ?,
                    retry_count = ?,
                    error_code = ?,
                    completed_at = CURRENT_TIMESTAMP(6),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND user_id = ?
                  AND type = 'PURCHASE_UTILITY'
                  AND status = 'PROCESSING'
                """,
                fallbackJson,
                latencyMs,
                retryCount,
                errorCode,
                jobId,
                userId
        );

        return updated == 1;
    }

    public Optional<PurchaseUtilityAiJobData>
    findRecentSucceededByInputHash(
            Long userId,
            String inputHash,
            String promptVersion,
            String model
    ) {
        requireText(inputHash, "inputHash");
        requireText(promptVersion, "promptVersion");
        requireText(model, "model");

        return jdbcTemplate.query(
                selectSql()
                        + """
                         WHERE user_id = ?
                           AND type = 'PURCHASE_UTILITY'
                           AND input_hash = ?
                           AND prompt_version = ?
                           AND model = ?
                           AND status = 'SUCCEEDED'
                           AND completed_at >=
                               CURRENT_TIMESTAMP(6) - INTERVAL 24 HOUR
                         ORDER BY completed_at DESC, id DESC
                         LIMIT 1
                        """,
                this::map,
                userId,
                inputHash,
                promptVersion,
                model
        ).stream().findFirst();
    }

    public Optional<PurchaseUtilityAiJobData>
    findRecentSucceededByInputHash(
            Long userId,
            String inputHash,
            String promptVersion,
            String model,
            Instant completedAfter
    ) {
        requireText(inputHash, "inputHash");
        requireText(promptVersion, "promptVersion");
        requireText(model, "model");
        Objects.requireNonNull(
                completedAfter,
                "completedAfter는 null일 수 없습니다."
        );

        return jdbcTemplate.query(
                selectSql()
                        + """
                         WHERE user_id = ?
                           AND type = 'PURCHASE_UTILITY'
                           AND input_hash = ?
                           AND prompt_version = ?
                           AND model = ?
                           AND status = 'SUCCEEDED'
                           AND completed_at >= ?
                         ORDER BY completed_at DESC, id DESC
                         LIMIT 1
                        """,
                this::map,
                userId,
                inputHash,
                promptVersion,
                model,
                Timestamp.from(completedAfter)
        ).stream().findFirst();
    }

    private String selectSql() {
        return """
                SELECT
                    id,
                    user_id,
                    status,
                    idempotency_key,
                    model,
                    prompt_version,
                    input_hash,
                    result_json,
                    fallback_json,
                    retry_count,
                    error_code,
                    started_at,
                    completed_at,
                    created_at,
                    updated_at
                FROM ai_jobs
                """;
    }

    private PurchaseUtilityAiJobData map(
            java.sql.ResultSet resultSet,
            int rowNumber
    ) throws java.sql.SQLException {
        return new PurchaseUtilityAiJobData(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                PurchaseUtilityAiJobStatus.valueOf(
                        resultSet.getString("status")
                ),
                resultSet.getString("idempotency_key"),
                resultSet.getString("model"),
                resultSet.getString("prompt_version"),
                resultSet.getString("input_hash"),
                resultSet.getString("result_json"),
                resultSet.getString("fallback_json"),
                resultSet.getInt("retry_count"),
                resultSet.getString("error_code"),
                toInstant(resultSet.getTimestamp("started_at")),
                toInstant(resultSet.getTimestamp("completed_at")),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at"))
        );
    }

    private Instant toInstant(
            Timestamp timestamp
    ) {
        return timestamp == null
                ? null
                : timestamp.toInstant();
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

    private void requireJsonText(
            String value,
            String field
    ) {
        requireText(value, field);
    }
}
