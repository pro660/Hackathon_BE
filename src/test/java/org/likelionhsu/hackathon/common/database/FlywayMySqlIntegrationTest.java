package org.likelionhsu.hackathon.common.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@Tag("integration")
class FlywayMySqlIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void projectMigrationsRunOnMySql() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                )
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()

        ) {
            ResultSet tableResult = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_name IN (
                          'users',
                          'local_credentials',
                          'social_accounts',
                          'pending_social_signups',
                          'terms_agreements',
                          'email_verifications',
                          'refresh_tokens',
                          'ai_jobs'
                      )
                    """);

            tableResult.next();
            assertThat(tableResult.getInt(1)).isEqualTo(8);

            ResultSet historyResult = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history
                    WHERE version IN ('1', '2')
                      AND success = 1
                    """);

            historyResult.next();
            assertThat(historyResult.getInt(1)).isEqualTo(2);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO ai_jobs (
                        user_id,
                        type,
                        status,
                        idempotency_key,
                        model,
                        prompt_version,
                        input_hash,
                        created_at,
                        updated_at
                    ) VALUES (
                        999999,
                        'STYLE_PLAN',
                        'PENDING',
                        'fk-test-key',
                        'gpt-5.6-luna',
                        'v1',
                        'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);

            statement.executeUpdate("""
                    INSERT INTO users (
                        id,
                        nickname,
                        gender,
                        status,
                        created_at,
                        updated_at
                    ) VALUES (
                        1,
                        'unique-test-user',
                        'NOT_SPECIFIED',
                        'ACTIVE',
                        CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP
                    )
                    """);

            statement.executeUpdate("""
                    INSERT INTO ai_jobs (
                        user_id,
                        type,
                        status,
                        idempotency_key,
                        model,
                        prompt_version,
                        input_hash,
                        created_at,
                        updated_at
                    ) VALUES (
                        1,
                        'STYLE_PLAN',
                        'PENDING',
                        'same-idempotency-key',
                        'gpt-5.6-luna',
                        'v1',
                        'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO ai_jobs (
                        user_id,
                        type,
                        status,
                        idempotency_key,
                        model,
                        prompt_version,
                        input_hash,
                        created_at,
                        updated_at
                    ) VALUES (
                        1,
                        'STYLE_PLAN',
                        'PENDING',
                        'same-idempotency-key',
                        'gpt-5.6-luna',
                        'v1',
                        'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);

            statement.executeUpdate("""
                    INSERT INTO ai_jobs (
                        user_id,
                        type,
                        status,
                        idempotency_key,
                        model,
                        prompt_version,
                        input_hash,
                        result_json,
                        fallback_json,
                        created_at,
                        updated_at
                    ) VALUES (
                        1,
                        'STYLE_PLAN',
                        'SUCCEEDED',
                        'json-test-key',
                        'gpt-5.6-luna',
                        'v1',
                        'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd',
                        JSON_OBJECT(
                            'title', '오늘의 스타일',
                            'score', 95
                        ),
                        JSON_OBJECT(
                            'reason', 'fallback-test'
                        ),
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """);

            ResultSet jsonResult = statement.executeQuery("""
                    SELECT
                        JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.title')) AS title,
                        JSON_EXTRACT(result_json, '$.score') AS score,
                        JSON_UNQUOTE(JSON_EXTRACT(fallback_json, '$.reason')) AS fallback_reason
                    FROM ai_jobs
                    WHERE user_id = 1
                      AND idempotency_key = 'json-test-key'
                    """);

            jsonResult.next();

            assertThat(jsonResult.getString("title"))
                    .isEqualTo("오늘의 스타일");

            assertThat(jsonResult.getInt("score"))
                    .isEqualTo(95);

            assertThat(jsonResult.getString("fallback_reason"))
                    .isEqualTo("fallback-test");

            statement.executeUpdate("""
                    INSERT INTO ai_jobs (
                        user_id,
                        type,
                        status,
                        idempotency_key,
                        model,
                        prompt_version,
                        input_hash,
                        created_at,
                        updated_at
                    ) VALUES (
                        1,
                        'STYLE_PLAN',
                        'PENDING',
                        'retry-default-test-key',
                        'gpt-5.6-luna',
                        'v1',
                        'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """);

            ResultSet retryDefaultResult = statement.executeQuery("""
                    SELECT retry_count
                    FROM ai_jobs
                    WHERE user_id = 1
                      AND idempotency_key = 'retry-default-test-key'
                    """);

            retryDefaultResult.next();
            assertThat(retryDefaultResult.getInt("retry_count"))
                    .isEqualTo(0);

            assertThatThrownBy(() -> statement.executeUpdate("""
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
                    ) VALUES (
                        1,
                        'STYLE_PLAN',
                        'PENDING',
                        'retry-check-test-key',
                        'gpt-5.6-luna',
                        'v1',
                        'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff',
                        2,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO ai_jobs (
                        user_id,
                        type,
                        status,
                        idempotency_key,
                        model,
                        prompt_version,
                        input_hash,
                        input_tokens,
                        created_at,
                        updated_at
                    ) VALUES (
                        1,
                        'STYLE_PLAN',
                        'PENDING',
                        'negative-input-tokens-test-key',
                        'gpt-5.6-luna',
                        'v1',
                        '1111111111111111111111111111111111111111111111111111111111111111',
                        -1,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO ai_jobs (
                        user_id,
                        type,
                        status,
                        idempotency_key,
                        model,
                        prompt_version,
                        input_hash,
                        output_tokens,
                        created_at,
                        updated_at
                    ) VALUES (
                        1,
                        'STYLE_PLAN',
                        'PENDING',
                        'negative-output-tokens-test-key',
                        'gpt-5.6-luna',
                        'v1',
                        '2222222222222222222222222222222222222222222222222222222222222222',
                        -1,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO ai_jobs (
                        user_id,
                        type,
                        status,
                        idempotency_key,
                        model,
                        prompt_version,
                        input_hash,
                        latency_ms,
                        created_at,
                        updated_at
                    ) VALUES (
                        1,
                        'STYLE_PLAN',
                        'PENDING',
                        'negative-latency-test-key',
                        'gpt-5.6-luna',
                        'v1',
                        '3333333333333333333333333333333333333333333333333333333333333333',
                        -1,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }
}