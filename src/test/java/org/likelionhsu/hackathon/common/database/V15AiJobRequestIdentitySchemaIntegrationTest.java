package org.likelionhsu.hackathon.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
class V15AiJobRequestIdentitySchemaIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void requestHashIsAddedAndInputHashBecomesNullable()
            throws Exception {
        migrateToV14();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            insertUser(statement);
            insertLegacyAiJob(statement);
        }

        migrateToLatest();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            assertColumnNullable(
                    statement,
                    "request_hash",
                    true
            );
            assertColumnNullable(
                    statement,
                    "input_hash",
                    true
            );
            assertLegacyRowPreserved(statement);
            assertNewPendingJobAllowsNullInputHash(statement);
            assertMigrationApplied(statement);
        }
    }

    private void migrateToV14() {
        Flyway.configure()
                .dataSource(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                )
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("14"))
                .load()
                .migrate();
    }

    private void migrateToLatest() {
        Flyway.configure()
                .dataSource(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                )
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private void insertUser(Statement statement) throws Exception {
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
                    'v15-ai-job-test-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);
    }

    private void insertLegacyAiJob(Statement statement)
            throws Exception {
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
                    'PURCHASE_UTILITY',
                    'PENDING',
                    'legacy-v14-job',
                    'legacy-model',
                    'legacy-prompt-v1',
                    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);
    }

    private void assertColumnNullable(
            Statement statement,
            String columnName,
            boolean expectedNullable
    ) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT
                    is_nullable,
                    character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'ai_jobs'
                  AND column_name = '%s'
                """.formatted(columnName))) {

            assertThat(result.next()).isTrue();
            assertThat(result.getString("is_nullable"))
                    .isEqualTo(expectedNullable ? "YES" : "NO");
            assertThat(result.getInt("character_maximum_length"))
                    .isEqualTo(64);
        }
    }

    private void assertLegacyRowPreserved(Statement statement)
            throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT request_hash, input_hash
                FROM ai_jobs
                WHERE idempotency_key = 'legacy-v14-job'
                """)) {

            assertThat(result.next()).isTrue();
            assertThat(result.getString("request_hash")).isNull();
            assertThat(result.getString("input_hash"))
                    .isEqualTo("a".repeat(64));
        }
    }

    private void assertNewPendingJobAllowsNullInputHash(
            Statement statement
    ) throws Exception {
        statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    user_id,
                    type,
                    status,
                    idempotency_key,
                    request_hash,
                    model,
                    prompt_version,
                    created_at,
                    updated_at
                ) VALUES (
                    1,
                    'PURCHASE_UTILITY',
                    'PENDING',
                    'new-v15-job',
                    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
                    'configured-model',
                    'purchase-utility-summary-v1',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        try (ResultSet result = statement.executeQuery("""
                SELECT request_hash, input_hash
                FROM ai_jobs
                WHERE idempotency_key = 'new-v15-job'
                """)) {

            assertThat(result.next()).isTrue();
            assertThat(result.getString("request_hash"))
                    .isEqualTo("b".repeat(64));
            assertThat(result.getString("input_hash")).isNull();
        }
    }

    private void assertMigrationApplied(Statement statement)
            throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '15'
                  AND success = 1
                """)) {

            result.next();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }
}
