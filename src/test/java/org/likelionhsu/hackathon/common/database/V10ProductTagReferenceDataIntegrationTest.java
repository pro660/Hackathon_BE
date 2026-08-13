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
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Tag("integration")
class V10ProductTagReferenceDataIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void productTagReferenceDataIsSeededExactly() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            Set<String> actualTags = new HashSet<>();

            try (ResultSet result = statement.executeQuery("""
                    SELECT type, code
                    FROM product_tags
                    """)) {

                while (result.next()) {
                    actualTags.add(
                            result.getString("type")
                                    + ":"
                                    + result.getString("code")
                    );
                }
            }

            assertThat(actualTags)
                    .containsExactlyInAnyOrder(
                            "STYLE:CASUAL",
                            "STYLE:FORMAL",
                            "STYLE:NEAT",
                            "STYLE:GLAMOROUS",

                            "SEASON:SPRING",
                            "SEASON:SUMMER",
                            "SEASON:AUTUMN",
                            "SEASON:WINTER",
                            "SEASON:ALL_SEASON",

                            "OCCASION:DAILY",
                            "OCCASION:DATE",
                            "OCCASION:TRAVEL",
                            "OCCASION:GATHERING",
                            "OCCASION:CEREMONY",
                            "OCCASION:OUTDOOR",
                            "OCCASION:OTHER",

                            "FEATURE:COMPACT",
                            "FEATURE:SPACIOUS",
                            "FEATURE:MULTIWAY"
                    );

            assertThat(actualTags)
                    .hasSize(19);
        }
    }

    @Test
    void productTagReferenceDataHasExpectedCountPerType() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            assertTagCount(statement, "STYLE", 4);
            assertTagCount(statement, "SEASON", 5);
            assertTagCount(statement, "OCCASION", 7);
            assertTagCount(statement, "FEATURE", 3);
        }
    }

    private void assertTagCount(
            Statement statement,
            String type,
            int expectedCount
    ) throws Exception {

        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM product_tags
                WHERE type = '%s'
                """.formatted(type))) {

            result.next();

            assertThat(result.getInt(1))
                    .isEqualTo(expectedCount);
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
