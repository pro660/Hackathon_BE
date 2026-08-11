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
class FlywayMySqlIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void flywayMigrationRunsOnMySql() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                )
                .locations("classpath:db/testmigration")
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
                      AND table_name = 'flyway_smoke_test'
                    """);

            tableResult.next();
            assertThat(tableResult.getInt(1)).isEqualTo(1);

            ResultSet historyResult = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history
                    WHERE version = '1'
                      AND success = 1
                    """);

            historyResult.next();
            assertThat(historyResult.getInt(1)).isEqualTo(1);
        }
    }
}
