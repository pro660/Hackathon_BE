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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Tag("integration")
class V13ReauthTokenSchemaIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void reauthTokenTableHasExpectedColumns() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = 'reauth_tokens'
                      AND column_name IN (
                          'id',
                          'user_id',
                          'purpose',
                          'token_hash',
                          'expires_at',
                          'consumed_at',
                          'created_at',
                          'updated_at'
                      )
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(8);
            }
        }
    }

    @Test
    void reauthTokenConstraintsAndIndexExist() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.table_constraints
                    WHERE constraint_schema = DATABASE()
                      AND table_name = 'reauth_tokens'
                      AND constraint_name IN (
                          'uk_reauth_tokens_token_hash',
                          'fk_reauth_tokens_user'
                      )
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(2);
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(DISTINCT index_name)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'reauth_tokens'
                      AND index_name =
                          'idx_reauth_tokens_user_purpose_expires_consumed'
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    private void migrateDatabase() {
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
}
