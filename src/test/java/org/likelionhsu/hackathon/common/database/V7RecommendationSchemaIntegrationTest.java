package org.likelionhsu.hackathon.common.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Tag("integration")
class V7RecommendationSchemaIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void recommendationUserForeignKeyIsEnforced() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO recommendations (
                        user_id,
                        generation_type,
                        context_json,
                        generated_at,
                        created_at,
                        updated_at
                    ) VALUES (
                        999999999,
                        'AI',
                        JSON_OBJECT(),
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void recommendationsAreDeletedWhenUserIsDeleted() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    300,
                    'v7-rec-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    300,
                    300,
                    'AI',
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM users
                WHERE id = 300
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM recommendations
                WHERE id = 300
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }
        }
    }

    @Test
    void recommendationAiJobIsSetNullWhenAiJobIsDeleted() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    301,
                    'v7-ai-job-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    id,
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
                    301,
                    301,
                    'PREFERENCE_ANALYSIS',
                    'COMPLETED',
                    'v7-recommendation-ai-job-301',
                    'test-model',
                    'v1',
                    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    ai_job_id,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    301,
                    301,
                    'AI',
                    JSON_OBJECT(),
                    301,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM ai_jobs
                WHERE id = 301
                """);

            try (var result = statement.executeQuery("""
                SELECT ai_job_id
                FROM recommendations
                WHERE id = 301
                """)) {

                result.next();

                assertThat(result.getObject("ai_job_id"))
                        .isNull();
            }
        }
    }

    @Test
    void recommendationAiJobForeignKeyIsEnforced() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    302,
                    'v7-ai-fk-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recommendations (
                    user_id,
                    generation_type,
                    context_json,
                    ai_job_id,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    302,
                    'AI',
                    JSON_OBJECT(),
                    999999999,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void duplicateRecommendationAiJobIsRejected() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    303,
                    'v7-ai-unique',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    id,
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
                    303,
                    303,
                    'PREFERENCE_ANALYSIS',
                    'COMPLETED',
                    'v7-ai-unique-303',
                    'test-model',
                    'v1',
                    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    ai_job_id,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    303,
                    303,
                    'AI',
                    JSON_OBJECT(),
                    303,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    ai_job_id,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    304,
                    303,
                    'AI',
                    JSON_OBJECT(),
                    303,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void duplicateRecommendationProductIsRejected() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    305,
                    'v7-rec-product',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    305,
                    'MCM',
                    'V7-PRODUCT-305',
                    'V7 Recommendation Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-product-305',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    305,
                    305,
                    'AI',
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    305,
                    305,
                    1,
                    95.00,
                    JSON_OBJECT()
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    305,
                    305,
                    2,
                    90.00,
                    JSON_OBJECT()
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void duplicateRecommendationRankIsRejected() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    306,
                    'v7-rank-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES
                (
                    306,
                    'MCM',
                    'V7-RANK-306',
                    'V7 Rank Product One',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-rank-306',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                ),
                (
                    307,
                    'MCM',
                    'V7-RANK-307',
                    'V7 Rank Product Two',
                    'BAG',
                    1100000,
                    'BROWN',
                    'LEATHER',
                    'https://example.com/v7-rank-307',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    306,
                    306,
                    'AI',
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    306,
                    306,
                    1,
                    95.00,
                    JSON_OBJECT()
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    306,
                    307,
                    1,
                    90.00,
                    JSON_OBJECT()
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void invalidRecommendationRankIsRejected() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    308,
                    'v7-rank-check',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    308,
                    'MCM',
                    'V7-RANK-CHECK-308',
                    'V7 Rank Check Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-rank-check-308',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    308,
                    308,
                    'AI',
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    308,
                    308,
                    0,
                    90.00,
                    JSON_OBJECT()
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void recommendationProductScoreOutsideRangeIsRejected() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    309,
                    'v7-score-check',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    309,
                    'MCM',
                    'V7-SCORE-CHECK-309',
                    'V7 Score Check Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-score-check-309',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    309,
                    309,
                    'AI',
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    309,
                    309,
                    1,
                    -0.01,
                    JSON_OBJECT()
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    309,
                    309,
                    2,
                    100.01,
                    JSON_OBJECT()
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void recommendationProductForeignKeysAreEnforced() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    310,
                    'v7-rp-fk-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    310,
                    'MCM',
                    'V7-RP-FK-310',
                    'V7 Recommendation FK Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-rp-fk-310',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    310,
                    310,
                    'AI',
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    999999999,
                    310,
                    1,
                    90.00,
                    JSON_OBJECT()
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    310,
                    999999999,
                    1,
                    90.00,
                    JSON_OBJECT()
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void recommendationProductsAreDeletedWhenRecommendationIsDeleted() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    311,
                    'v7-rp-cascade',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    311,
                    'MCM',
                    'V7-RP-CASCADE-311',
                    'V7 Cascade Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-rp-cascade-311',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    311,
                    311,
                    'AI',
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    id,
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    311,
                    311,
                    311,
                    1,
                    95.00,
                    JSON_OBJECT()
                )
                """);

            statement.executeUpdate("""
                DELETE FROM recommendations
                WHERE id = 311
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM recommendation_products
                WHERE id = 311
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM products
                WHERE id = 311
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void productDeletionIsBlockedWhileRecommendationProductReferencesIt() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    312,
                    'v7-product-lock',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    312,
                    'MCM',
                    'V7-PRODUCT-LOCK-312',
                    'V7 Restricted Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-product-lock-312',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendations (
                    id,
                    user_id,
                    generation_type,
                    context_json,
                    generated_at,
                    created_at,
                    updated_at
                ) VALUES (
                    312,
                    312,
                    'AI',
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO recommendation_products (
                    recommendation_id,
                    product_id,
                    rank_order,
                    score,
                    product_snapshot
                ) VALUES (
                    312,
                    312,
                    1,
                    95.00,
                    JSON_OBJECT()
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM products
                WHERE id = 312
                """))
                    .isInstanceOf(SQLException.class);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM products
                WHERE id = 312
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void purchaseUtilityScoreOutsideRangeIsRejected() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    313,
                    'v7-util-score',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    313,
                    'MCM',
                    'V7-UTILITY-313',
                    'V7 Utility Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-utility-313',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    313,
                    313,
                    -0.01,
                    0,
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    313,
                    313,
                    100.01,
                    0,
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void purchaseUtilityDuplicateSimilarityScoreConstraintIsEnforced() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    314,
                    'v7-dup-score',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    314,
                    'MCM',
                    'V7-DUP-SCORE-314',
                    'V7 Duplicate Score Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-dup-score-314',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    duplicate_similarity_score,
                    factor_json,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    314,
                    314,
                    80.00,
                    0,
                    NULL,
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    duplicate_similarity_score,
                    factor_json,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    314,
                    314,
                    80.00,
                    0,
                    -0.01,
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    duplicate_similarity_score,
                    factor_json,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    314,
                    314,
                    80.00,
                    0,
                    100.01,
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void purchaseUtilityAnalysisForeignKeysAreEnforced() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    315,
                    'v7-util-fk',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    315,
                    'MCM',
                    'V7-UTILITY-FK-315',
                    'V7 Utility FK Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-utility-fk-315',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    999999999,
                    315,
                    80.00,
                    0,
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    315,
                    999999999,
                    80.00,
                    0,
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    ai_job_id,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    315,
                    315,
                    80.00,
                    0,
                    JSON_OBJECT(),
                    999999999,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void purchaseUtilityAnalysesAreDeletedWhenUserIsDeleted() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    316,
                    'v7-util-cascade',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    316,
                    'MCM',
                    'V7-UTILITY-CASCADE-316',
                    'V7 Utility Cascade Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-utility-cascade-316',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    id,
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    316,
                    316,
                    316,
                    80.00,
                    0,
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM users
                WHERE id = 316
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM purchase_utility_analyses
                WHERE id = 316
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM products
                WHERE id = 316
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void productDeletionIsBlockedWhilePurchaseUtilityAnalysisReferencesIt() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    317,
                    'v7-util-restrict',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    317,
                    'MCM',
                    'V7-UTILITY-RESTRICT-317',
                    'V7 Utility Restricted Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-utility-restrict-317',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    id,
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    317,
                    317,
                    317,
                    80.00,
                    0,
                    JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM products
                WHERE id = 317
                """))
                    .isInstanceOf(SQLException.class);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM products
                WHERE id = 317
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void purchaseUtilityAnalysisAiJobIsSetNullWhenAiJobIsDeleted() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    318,
                    'v7-util-ai-job',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    318,
                    'MCM',
                    'V7-UTILITY-AI-318',
                    'V7 Utility AI Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-utility-ai-318',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    id,
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
                    318,
                    318,
                    'PURCHASE_UTILITY',
                    'COMPLETED',
                    'v7-utility-ai-job-318',
                    'test-model',
                    'v1',
                    'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    id,
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    ai_job_id,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    318,
                    318,
                    318,
                    80.00,
                    0,
                    JSON_OBJECT(),
                    318,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM ai_jobs
                WHERE id = 318
                """);

            try (var result = statement.executeQuery("""
                SELECT ai_job_id
                FROM purchase_utility_analyses
                WHERE id = 318
                """)) {

                result.next();

                assertThat(result.getObject("ai_job_id"))
                        .isNull();
            }

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM purchase_utility_analyses
                WHERE id = 318
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void duplicatePurchaseUtilityAnalysisAiJobIsRejected() throws Exception {
        migrateDatabase();

        try (
                Connection connection = DriverManager.getConnection(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    319,
                    'v7-util-unique',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO products (
                    id,
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                ) VALUES
                (
                    319,
                    'MCM',
                    'V7-UTILITY-UNIQUE-319',
                    'V7 Utility Product One',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v7-utility-unique-319',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                ),
                (
                    320,
                    'MCM',
                    'V7-UTILITY-UNIQUE-320',
                    'V7 Utility Product Two',
                    'BAG',
                    1100000,
                    'BROWN',
                    'LEATHER',
                    'https://example.com/v7-utility-unique-320',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    id,
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
                    319,
                    319,
                    'PURCHASE_UTILITY',
                    'COMPLETED',
                    'v7-utility-unique-319',
                    'test-model',
                    'v1',
                    'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    id,
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    ai_job_id,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    319,
                    319,
                    319,
                    80.00,
                    0,
                    JSON_OBJECT(),
                    319,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO purchase_utility_analyses (
                    id,
                    user_id,
                    product_id,
                    utility_score,
                    compatible_item_count,
                    factor_json,
                    ai_job_id,
                    analyzed_at,
                    created_at,
                    updated_at
                ) VALUES (
                    320,
                    319,
                    320,
                    75.00,
                    0,
                    JSON_OBJECT(),
                    319,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
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