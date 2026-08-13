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
class V11UserItemCareSchemaIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void userItemStatusAndCareRecordsAreRemoved() throws Exception {
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
                      AND table_name = 'user_items'
                      AND column_name = 'status'
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isZero();
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'user_items'
                      AND index_name = 'idx_user_items_user_status'
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isZero();
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_name = 'care_records'
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isZero();
            }
        }
    }

    @Test
    void nextCareDateIsNullableDateColumn() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT data_type, is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = 'user_items'
                          AND column_name = 'next_care_date'
                        """)
        ) {
            assertThat(result.next())
                    .isTrue();

            assertThat(result.getString("data_type"))
                    .isEqualTo("date");

            assertThat(result.getString("is_nullable"))
                    .isEqualTo("YES");

            assertThat(result.next())
                    .isFalse();
        }
    }

    @Test
    void retainedWearAndImageStatusSchemaStillExists() throws Exception {
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
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_name IN (
                          'wear_records',
                          'wear_record_items'
                      )
                    """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(2);
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = 'image_assets'
                      AND column_name = 'status'
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
