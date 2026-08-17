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
class CartItemFlywayIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_cart_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void v19CreatesCartItemsTable() throws Exception {
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
                Connection connection =
                        DriverManager.getConnection(
                                mysql.getJdbcUrl(),
                                mysql.getUsername(),
                                mysql.getPassword()
                        );
                Statement statement =
                        connection.createStatement()
        ) {
            try (ResultSet resultSet =
                         statement.executeQuery(
                                 """
                                 SELECT COUNT(*)
                                 FROM information_schema.tables
                                 WHERE table_schema = DATABASE()
                                   AND table_name = 'cart_items'
                                 """
                         )) {
                resultSet.next();
                assertThat(resultSet.getInt(1))
                        .isEqualTo(1);
            }

            try (ResultSet resultSet =
                         statement.executeQuery(
                                 """
                                 SELECT COUNT(DISTINCT index_name)
                                 FROM information_schema.statistics
                                 WHERE table_schema = DATABASE()
                                   AND table_name = 'cart_items'
                                   AND index_name = 'uk_cart_items_user_product'
                                   AND non_unique = 0
                                 """
                         )) {
                resultSet.next();
                assertThat(resultSet.getInt(1))
                        .isEqualTo(1);
            }

            try (ResultSet historyResult =
                         statement.executeQuery(
                                 """
                                 SELECT COUNT(*)
                                 FROM flyway_schema_history
                                 WHERE version = '19'
                                   AND success = 1
                                 """
                         )) {
                historyResult.next();
                assertThat(historyResult.getInt(1))
                        .isEqualTo(1);
            }
        }
    }
}