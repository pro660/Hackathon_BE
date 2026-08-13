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
class V12PurchaseUtilitySchemaIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void duplicateSimilarityScoreColumnAndConstraintAreRemoved() throws Exception {
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
                      AND table_name = 'purchase_utility_analyses'
                      AND column_name = 'duplicate_similarity_score'
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isZero();
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.table_constraints
                    WHERE constraint_schema = DATABASE()
                      AND table_name = 'purchase_utility_analyses'
                      AND constraint_name =
                          'chk_purchase_utility_analyses_duplicate_similarity_score'
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isZero();
            }
        }
    }

    @Test
    void retainedPurchaseUtilitySchemaStillExists() throws Exception {
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
                      AND table_name = 'purchase_utility_analyses'
                      AND column_name IN (
                          'utility_score',
                          'compatible_item_count',
                          'factor_json',
                          'summary',
                          'ai_job_id'
                      )
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(5);
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.table_constraints
                    WHERE constraint_schema = DATABASE()
                      AND table_name = 'purchase_utility_analyses'
                      AND constraint_name =
                          'chk_purchase_utility_analyses_utility_score'
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
