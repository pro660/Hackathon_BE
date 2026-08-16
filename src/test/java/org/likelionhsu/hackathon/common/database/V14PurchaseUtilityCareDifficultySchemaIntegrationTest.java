package org.likelionhsu.hackathon.common.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
class V14PurchaseUtilityCareDifficultySchemaIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void careDifficultyIsBackfilledAndConstrained() throws Exception {
        migrateToV13();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            insertUser(statement);

            long leatherProductId =
                    insertProduct(
                            statement,
                            "CARE-BACKFILL-LEATHER",
                            "LEATHER"
                    );
            long nylonProductId =
                    insertProduct(
                            statement,
                            "CARE-BACKFILL-NYLON",
                            "NYLON"
                    );
            long canvasProductId =
                    insertProduct(
                            statement,
                            "CARE-BACKFILL-CANVAS",
                            "CANVAS"
                    );
            long unknownProductId =
                    insertProduct(
                            statement,
                            "CARE-BACKFILL-NULL",
                            null
                    );

            insertAnalysis(statement, leatherProductId);
            insertAnalysis(statement, nylonProductId);
            insertAnalysis(statement, canvasProductId);
            insertAnalysis(statement, unknownProductId);
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
            assertBackfilledValues(statement);
            assertColumnIsNotNullable(statement);
            assertCheckConstraintExists(statement);

            assertThatThrownBy(() ->
                    statement.executeUpdate("""
                            UPDATE purchase_utility_analyses
                            SET care_difficulty = 'IMPOSSIBLE'
                            LIMIT 1
                            """)
            ).isInstanceOf(SQLException.class);
        }
    }

    private void migrateToV13() {
        Flyway.configure()
                .dataSource(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                )
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("13"))
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

    private void insertUser(Statement statement) throws SQLException {
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
                    'care-backfill-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);
    }

    private long insertProduct(
            Statement statement,
            String sku,
            String material
    ) throws SQLException {
        String materialSql =
                material == null
                        ? "NULL"
                        : "'" + material + "'";

        statement.executeUpdate("""
                INSERT INTO products (
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    material,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    'MCM',
                    '%s',
                    'Care Backfill Product',
                    'BAG',
                    100000,
                    %s,
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """.formatted(sku, materialSql));

        try (ResultSet result = statement.executeQuery("""
                SELECT id
                FROM products
                WHERE sku = '%s'
                """.formatted(sku))) {

            result.next();
            return result.getLong("id");
        }
    }

    private void insertAnalysis(
            Statement statement,
            long productId
    ) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    summary,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    1,
                    %d,
                    70.00,
                    0,
                    JSON_OBJECT(
                        'ruleVersion',
                        'purchase-utility-rule-v1'
                    ),
                    'backfill-test',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """.formatted(productId));
    }

    private void assertBackfilledValues(
            Statement statement
    ) throws SQLException {
        Map<String, String> values = new LinkedHashMap<>();

        try (ResultSet result = statement.executeQuery("""
                SELECT p.sku, pua.care_difficulty
                FROM purchase_utility_analyses pua
                JOIN products p
                    ON p.id = pua.product_id
                WHERE p.sku LIKE 'CARE-BACKFILL-%'
                ORDER BY p.sku
                """)) {

            while (result.next()) {
                values.put(
                        result.getString("sku"),
                        result.getString("care_difficulty")
                );
            }
        }

        assertThat(values)
                .containsEntry("CARE-BACKFILL-LEATHER", "HARD")
                .containsEntry("CARE-BACKFILL-NYLON", "EASY")
                .containsEntry("CARE-BACKFILL-CANVAS", "MODERATE")
                .containsEntry("CARE-BACKFILL-NULL", "UNKNOWN");
    }

    private void assertColumnIsNotNullable(
            Statement statement
    ) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'purchase_utility_analyses'
                  AND column_name = 'care_difficulty'
                """)) {

            result.next();
            assertThat(result.getString("is_nullable"))
                    .isEqualTo("NO");
        }
    }

    private void assertCheckConstraintExists(
            Statement statement
    ) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name = 'purchase_utility_analyses'
                  AND constraint_name =
                      'chk_purchase_utility_analyses_care_difficulty'
                """)) {

            result.next();
            assertThat(result.getInt(1))
                    .isEqualTo(1);
        }
    }
}
