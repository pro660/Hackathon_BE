package org.likelionhsu.hackathon.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
class ProductPassportFlywayIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_passport_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void v17AddsNullablePurchaseMetadataToUserItems() throws Exception {
        Flyway.configure()
                .dataSource(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                )
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT column_name, is_nullable, character_maximum_length
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = 'user_items'
                      AND column_name IN (
                          'purchase_order_number',
                          'purchase_place'
                      )
                    ORDER BY column_name
                    """)) {
                resultSet.next();
                assertThat(resultSet.getString("column_name"))
                        .isEqualTo("purchase_order_number");
                assertThat(resultSet.getString("is_nullable"))
                        .isEqualTo("YES");
                assertThat(resultSet.getLong("character_maximum_length"))
                        .isEqualTo(100L);

                resultSet.next();
                assertThat(resultSet.getString("column_name"))
                        .isEqualTo("purchase_place");
                assertThat(resultSet.getString("is_nullable"))
                        .isEqualTo("YES");
                assertThat(resultSet.getLong("character_maximum_length"))
                        .isEqualTo(200L);
            }

            try (ResultSet historyResult = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history
                    WHERE version = '17'
                      AND success = 1
                    """)) {
                historyResult.next();
                assertThat(historyResult.getInt(1)).isEqualTo(1);
            }
        }
    }
}
