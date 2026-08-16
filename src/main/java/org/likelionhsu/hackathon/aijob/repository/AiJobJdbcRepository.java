package org.likelionhsu.hackathon.aijob.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AiJobJdbcRepository {

    private static final int SHA_256_HEX_LENGTH = 64;

    private static final String SELECT_SQL = """
            SELECT
                id,
                user_id,
                type,
                status,
                idempotency_key,
                request_hash,
                model,
                prompt_version,
                input_hash,
                result_json,
                fallback_json,
                input_tokens,
                output_tokens,
                latency_ms,
                retry_count,
                error_code,
                started_at,
                completed_at,
                created_at,
                updated_at
            FROM ai_jobs
            """;

    private final JdbcTemplate jdbcTemplate;

    public AiJobJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createPending(
            Long userId,
            AiJobType type,
            String idempotencyKey,
            String requestHash,
            String model,
            String promptVersion
    ) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(type, "type은 null일 수 없습니다.");
        requireText(idempotencyKey, "idempotencyKey");
        requireSha256Hash(requestHash, "requestHash");
        requireText(model, "model");
        requireText(promptVersion, "promptVersion");

        KeyHolder keyHolder = new GeneratedKeyHolder();

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
                                        request_hash,
                                        model,
                                        prompt_version,
                                        input_hash,
                                        retry_count,
                                        created_at,
                                        updated_at
                                    )
                                    VALUES (
                                        ?,
                                        ?,
                                        'PENDING',
                                        ?,
                                        ?,
                                        ?,
                                        ?,
                                        NULL,
                                        0,
                                        CURRENT_TIMESTAMP(6),
                                        CURRENT_TIMESTAMP(6)
                                    )
                                    """,
                                    Statement.RETURN_GENERATED_KEYS
                            );

                    statement.setLong(1, userId);
                    statement.setString(2, type.name());
                    statement.setString(3, idempotencyKey);
                    statement.setString(4, requestHash);
                    statement.setString(5, model);
                    statement.setString(6, promptVersion);

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

    public Optional<AiJobData> findByUserAndIdempotencyKey(
            Long userId,
            String idempotencyKey
    ) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        requireText(idempotencyKey, "idempotencyKey");

        return jdbcTemplate.query(
                SELECT_SQL
                        + """
                         WHERE user_id = ?
                           AND idempotency_key = ?
                         LIMIT 1
                        """,
                this::map,
                userId,
                idempotencyKey
        ).stream().findFirst();
    }

    public Optional<AiJobData> findOwned(
            Long userId,
            Long jobId
    ) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(jobId, "jobId는 null일 수 없습니다.");

        return jdbcTemplate.query(
                SELECT_SQL
                        + """
                         WHERE id = ?
                           AND user_id = ?
                         LIMIT 1
                        """,
                this::map,
                jobId,
                userId
        ).stream().findFirst();
    }

    private AiJobData map(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        return new AiJobData(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                AiJobType.valueOf(resultSet.getString("type")),
                AiJobStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("idempotency_key"),
                resultSet.getString("request_hash"),
                resultSet.getString("model"),
                resultSet.getString("prompt_version"),
                resultSet.getString("input_hash"),
                resultSet.getString("result_json"),
                resultSet.getString("fallback_json"),
                nullableInteger(resultSet, "input_tokens"),
                nullableInteger(resultSet, "output_tokens"),
                nullableLong(resultSet, "latency_ms"),
                resultSet.getInt("retry_count"),
                resultSet.getString("error_code"),
                toInstant(resultSet.getTimestamp("started_at")),
                toInstant(resultSet.getTimestamp("completed_at")),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at"))
        );
    }

    private Integer nullableInteger(
            ResultSet resultSet,
            String column
    ) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private Long nullableLong(
            ResultSet resultSet,
            String column
    ) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private Instant toInstant(Timestamp timestamp) {
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

    private void requireSha256Hash(
            String value,
            String field
    ) {
        requireText(value, field);

        if (value.length() != SHA_256_HEX_LENGTH) {
            throw new IllegalArgumentException(
                    field + "는 SHA-256 hex 64자여야 합니다."
            );
        }
    }
}
