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

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void projectMigrationsRunOnMySql() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                )
                .locations("classpath:db/migration")
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
            verifyMigrationState(statement);

            verifyAiJobConstraints(statement);
            verifyPreferenceProfileConstraints(statement);

            long productId = verifyProductConstraints(statement);
            verifyProductImageConstraints(statement, productId);

            long productTagId = verifyProductTagConstraints(statement);
            verifyProductTagMappingConstraints(
                    statement,
                    productId,
                    productTagId
            );

            verifyWishlistConstraints(statement, productId);
        }
    }

    private void verifyMigrationState(Statement statement) throws SQLException {
        var foundTables = new java.util.ArrayList<String>();

        try (ResultSet tableResult = statement.executeQuery("""
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name IN (
                              'users',
                              'local_credentials',
                              'social_accounts',
                              'pending_social_signups',
                              'terms_agreements',
                              'email_verifications',
                              'refresh_tokens',
                              'ai_jobs',
                              'products',
                              'product_images',
                              'product_tags',
                              'product_tag_mappings',
                              'preference_profiles',
                              'wishlists',
                              'user_items',
                              'image_assets',
                              'wear_records',
                              'wear_record_items',
                              'care_records',
                              'recommendations',
                              'recommendation_products',
                              'purchase_utility_analyses',
                              'places',
                              'saved_places',
                              'style_plans',
                              'style_plan_items',
                              'style_plan_products',
                              'style_plan_places'
                          )
                        ORDER BY table_name
                        """)) {

            while (tableResult.next()) {
                foundTables.add(tableResult.getString("table_name"));
            }
        }

        assertThat(foundTables)
                .containsExactlyInAnyOrder(
                        "users",
                        "local_credentials",
                        "social_accounts",
                        "pending_social_signups",
                        "terms_agreements",
                        "email_verifications",
                        "refresh_tokens",
                        "ai_jobs",
                        "products",
                        "product_images",
                        "product_tags",
                        "product_tag_mappings",
                        "preference_profiles",
                        "wishlists",
                        "user_items",
                        "image_assets",
                        "wear_records",
                        "wear_record_items",
                        "care_records",
                        "recommendations",
                        "recommendation_products",
                        "purchase_utility_analyses",
                        "places",
                        "saved_places",
                        "style_plans",
                        "style_plan_items",
                        "style_plan_products",
                        "style_plan_places"
                );

        try (ResultSet historyResult = statement.executeQuery("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8', '9')
                  AND success = 1
                """)) {

            historyResult.next();
            assertThat(historyResult.getInt(1)).isEqualTo(9);
        }

        try (ResultSet columnResult = statement.executeQuery("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'product_tags'
                  AND column_name = 'display_name'
                """)) {

            columnResult.next();
            assertThat(columnResult.getInt(1)).isZero();
        }
    }

    private void verifyAiJobConstraints(Statement statement) throws SQLException {
        assertThatThrownBy(() -> statement.executeUpdate("""
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
                    999999,
                    'STYLE_PLAN',
                    'PENDING',
                    'fk-test-key',
                    'gpt-5.6-luna',
                    'v1',
                    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

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
                    'unique-test-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """);

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
                    'STYLE_PLAN',
                    'PENDING',
                    'same-idempotency-key',
                    'gpt-5.6-luna',
                    'v1',
                    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        assertThatThrownBy(() -> statement.executeUpdate("""
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
                    'STYLE_PLAN',
                    'PENDING',
                    'same-idempotency-key',
                    'gpt-5.6-luna',
                    'v1',
                    'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    user_id,
                    type,
                    status,
                    idempotency_key,
                    model,
                    prompt_version,
                    input_hash,
                    result_json,
                    fallback_json,
                    created_at,
                    updated_at
                ) VALUES (
                    1,
                    'STYLE_PLAN',
                    'SUCCEEDED',
                    'json-test-key',
                    'gpt-5.6-luna',
                    'v1',
                    'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd',
                    JSON_OBJECT(
                        'title', '오늘의 스타일',
                        'score', 95
                    ),
                    JSON_OBJECT(
                        'reason', 'fallback-test'
                    ),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        try (ResultSet jsonResult = statement.executeQuery("""
                SELECT
                    JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.title')) AS title,
                    JSON_EXTRACT(result_json, '$.score') AS score,
                    JSON_UNQUOTE(JSON_EXTRACT(fallback_json, '$.reason')) AS fallback_reason
                FROM ai_jobs
                WHERE user_id = 1
                  AND idempotency_key = 'json-test-key'
                """)) {

            jsonResult.next();

            assertThat(jsonResult.getString("title"))
                    .isEqualTo("오늘의 스타일");

            assertThat(jsonResult.getInt("score"))
                    .isEqualTo(95);

            assertThat(jsonResult.getString("fallback_reason"))
                    .isEqualTo("fallback-test");
        }

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
                    'STYLE_PLAN',
                    'PENDING',
                    'retry-default-test-key',
                    'gpt-5.6-luna',
                    'v1',
                    'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        try (ResultSet retryDefaultResult = statement.executeQuery("""
                SELECT retry_count
                FROM ai_jobs
                WHERE user_id = 1
                  AND idempotency_key = 'retry-default-test-key'
                """)) {

            retryDefaultResult.next();

            assertThat(retryDefaultResult.getInt("retry_count"))
                    .isEqualTo(0);
        }

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    user_id,
                    type,
                    status,
                    idempotency_key,
                    model,
                    prompt_version,
                    input_hash,
                    retry_count,
                    created_at,
                    updated_at
                ) VALUES (
                    1,
                    'STYLE_PLAN',
                    'PENDING',
                    'retry-check-test-key',
                    'gpt-5.6-luna',
                    'v1',
                    'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff',
                    2,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    user_id,
                    type,
                    status,
                    idempotency_key,
                    model,
                    prompt_version,
                    input_hash,
                    input_tokens,
                    created_at,
                    updated_at
                ) VALUES (
                    1,
                    'STYLE_PLAN',
                    'PENDING',
                    'negative-input-tokens-test-key',
                    'gpt-5.6-luna',
                    'v1',
                    '1111111111111111111111111111111111111111111111111111111111111111',
                    -1,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    user_id,
                    type,
                    status,
                    idempotency_key,
                    model,
                    prompt_version,
                    input_hash,
                    output_tokens,
                    created_at,
                    updated_at
                ) VALUES (
                    1,
                    'STYLE_PLAN',
                    'PENDING',
                    'negative-output-tokens-test-key',
                    'gpt-5.6-luna',
                    'v1',
                    '2222222222222222222222222222222222222222222222222222222222222222',
                    -1,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO ai_jobs (
                    user_id,
                    type,
                    status,
                    idempotency_key,
                    model,
                    prompt_version,
                    input_hash,
                    latency_ms,
                    created_at,
                    updated_at
                ) VALUES (
                    1,
                    'STYLE_PLAN',
                    'PENDING',
                    'negative-latency-test-key',
                    'gpt-5.6-luna',
                    'v1',
                    '3333333333333333333333333333333333333333333333333333333333333333',
                    -1,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);
    }

    private long verifyProductConstraints(Statement statement) throws SQLException {
        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO products (
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    'MCM',
                    'PRICE-CHECK-TEST',
                    'Price Check Test Product',
                    'BAG',
                    -1,
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        statement.executeUpdate("""
                INSERT INTO products (
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    'MCM',
                    'SKU-UNIQUE-TEST',
                    'First SKU Test Product',
                    'BAG',
                    100000,
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO products (
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    'MCM',
                    'SKU-UNIQUE-TEST',
                    'Second SKU Test Product',
                    'BAG',
                    200000,
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        try (ResultSet productResult = statement.executeQuery("""
                SELECT id
                FROM products
                WHERE sku = 'SKU-UNIQUE-TEST'
                """)) {

            productResult.next();
            return productResult.getLong("id");
        }
    }

    private void verifyProductImageConstraints(
                Statement statement,
                long productId
        ) throws SQLException {

        statement.executeUpdate("""
                INSERT INTO product_images (
                    product_id,
                    url,
                    sort_order,
                    is_primary,
                    created_at,
                    updated_at
                ) VALUES (
                    %d,
                    'https://example.com/image-1.jpg',
                    0,
                    TRUE,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """.formatted(productId));

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO product_images (
                    product_id,
                    url,
                    sort_order,
                    is_primary,
                    created_at,
                    updated_at
                ) VALUES (
                    %d,
                    'https://example.com/image-2.jpg',
                    0,
                    FALSE,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """.formatted(productId)))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO product_images (
                    product_id,
                    url,
                    sort_order,
                    is_primary,
                    created_at,
                    updated_at
                ) VALUES (
                    %d,
                    'https://example.com/negative-sort-order.jpg',
                    -1,
                    FALSE,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """.formatted(productId)))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO product_images (
                    product_id,
                    url,
                    sort_order,
                    is_primary,
                    created_at,
                    updated_at
                ) VALUES (
                    999999999,
                    'https://example.com/invalid-product.jpg',
                    0,
                    FALSE,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        statement.executeUpdate("""
                INSERT INTO products (
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    'MCM',
                    'CASCADE-IMAGE-TEST',
                    'Cascade Image Test Product',
                    'BAG',
                    100000,
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        long cascadeProductId;

        try (ResultSet cascadeProductResult = statement.executeQuery("""
                SELECT id
                FROM products
                WHERE sku = 'CASCADE-IMAGE-TEST'
                """)) {

                cascadeProductResult.next();
                cascadeProductId = cascadeProductResult.getLong("id");
        }

        statement.executeUpdate("""
                INSERT INTO product_images (
                    product_id,
                    url,
                    sort_order,
                    is_primary,
                    created_at,
                    updated_at
                ) VALUES (
                    %d,
                    'https://example.com/cascade-image.jpg',
                    0,
                    TRUE,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """.formatted(cascadeProductId));

        statement.executeUpdate("""
                DELETE FROM products
                WHERE id = %d
                """.formatted(cascadeProductId));

        try (ResultSet cascadeImageResult = statement.executeQuery("""
                SELECT COUNT(*)
                FROM product_images
                WHERE product_id = %d
                """.formatted(cascadeProductId))) {

                cascadeImageResult.next();

                assertThat(cascadeImageResult.getInt(1))
                        .isEqualTo(0);
        }
    }

    private long verifyProductTagConstraints(Statement statement) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO product_tags (
                    type,
                    code
                ) VALUES (
                    'STYLE',
                    'TEST_UNIQUE_TAG'
                )
                """);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO product_tags (
                    type,
                    code
                ) VALUES (
                    'STYLE',
                    'TEST_UNIQUE_TAG'
                )
                """))
                .isInstanceOf(SQLException.class);

        try (ResultSet productTagResult = statement.executeQuery("""
                SELECT id
                FROM product_tags
                WHERE type = 'STYLE'
                  AND code = 'TEST_UNIQUE_TAG'
                """)) {

            productTagResult.next();
            return productTagResult.getLong("id");
        }
    }

    private void verifyProductTagMappingConstraints(
            Statement statement,
            long productId,
            long productTagId
    ) throws SQLException {

        statement.executeUpdate("""
                INSERT INTO product_tag_mappings (
                    product_id,
                    product_tag_id
                ) VALUES (
                    %d,
                    %d
                )
                """.formatted(productId, productTagId));

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO product_tag_mappings (
                    product_id,
                    product_tag_id
                ) VALUES (
                    %d,
                    %d
                )
                """.formatted(productId, productTagId)))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO product_tag_mappings (
                    product_id,
                    product_tag_id
                ) VALUES (
                    999999999,
                    %d
                )
                """.formatted(productTagId)))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO product_tag_mappings (
                    product_id,
                    product_tag_id
                ) VALUES (
                    %d,
                    999999999
                )
                """.formatted(productId)))
                .isInstanceOf(SQLException.class);

        statement.executeUpdate("""
            INSERT INTO products (
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    'MCM',
                    'CASCADE-TAG-MAPPING-TEST',
                    'Cascade Tag Mapping Test Product',
                    'BAG',
                    100000,
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        long cascadeTagProductId;

        try (ResultSet cascadeTagProductResult = statement.executeQuery("""
                SELECT id
                FROM products
                WHERE sku = 'CASCADE-TAG-MAPPING-TEST'
                """)) {

            cascadeTagProductResult.next();
            cascadeTagProductId = cascadeTagProductResult.getLong("id");
        }

        statement.executeUpdate("""
                INSERT INTO product_tag_mappings (
                    product_id,
                    product_tag_id
                ) VALUES (
                    %d,
                    %d
                )
                """.formatted(cascadeTagProductId, productTagId));

        statement.executeUpdate("""
                DELETE FROM products
                WHERE id = %d
                """.formatted(cascadeTagProductId));

        try (ResultSet cascadeTagMappingResult = statement.executeQuery("""
                SELECT COUNT(*)
                FROM product_tag_mappings
                WHERE product_id = %d
                """.formatted(cascadeTagProductId))) {

            cascadeTagMappingResult.next();

            assertThat(cascadeTagMappingResult.getInt(1))
                    .isEqualTo(0);
        }

        assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM product_tags
                WHERE id = %d
                """.formatted(productTagId)))
                .isInstanceOf(SQLException.class);
    }

    private void verifyPreferenceProfileConstraints(Statement statement) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO preference_profiles (
                    user_id,
                    analysis_version,
                    created_at,
                    updated_at
                ) VALUES (
                    1,
                    'v1',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO preference_profiles (
                    user_id,
                    analysis_version,
                    created_at,
                    updated_at
                ) VALUES (
                    1,
                    'v2',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        statement.executeUpdate("""
                INSERT INTO users (
                    id,
                    nickname,
                    gender,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    2,
                    'confidence-test-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO preference_profiles (
                    user_id,
                    confidence,
                    analysis_version,
                    created_at,
                    updated_at
                ) VALUES (
                    2,
                    -0.0001,
                    'v1',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO preference_profiles (
                    user_id,
                    confidence,
                    analysis_version,
                    created_at,
                    updated_at
                ) VALUES (
                    2,
                    1.0001,
                    'v1',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        statement.executeUpdate("""
                INSERT INTO preference_profiles (
                    user_id,
                    analysis_version,
                    created_at,
                    updated_at
                ) VALUES (
                    2,
                    'json-default-v1',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        try (ResultSet jsonDefaultResult = statement.executeQuery("""
                SELECT
                    JSON_TYPE(preferred_colors) AS colors_type,
                    JSON_LENGTH(preferred_colors) AS colors_length,
                    JSON_TYPE(preferred_categories) AS categories_type,
                    JSON_LENGTH(preferred_categories) AS categories_length,
                    JSON_TYPE(preferred_style_tags) AS style_tags_type,
                    JSON_LENGTH(preferred_style_tags) AS style_tags_length
                FROM preference_profiles
                WHERE user_id = 2
                """)) {

            jsonDefaultResult.next();

            assertThat(jsonDefaultResult.getString("colors_type"))
                    .isEqualTo("ARRAY");
            assertThat(jsonDefaultResult.getInt("colors_length"))
                    .isEqualTo(0);

            assertThat(jsonDefaultResult.getString("categories_type"))
                    .isEqualTo("ARRAY");
            assertThat(jsonDefaultResult.getInt("categories_length"))
                    .isEqualTo(0);

            assertThat(jsonDefaultResult.getString("style_tags_type"))
                    .isEqualTo("ARRAY");
            assertThat(jsonDefaultResult.getInt("style_tags_length"))
                    .isEqualTo(0);
        }

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
                    2,
                    'PREFERENCE_ANALYSIS',
                    'SUCCEEDED',
                    'preference-ai-job-test-key',
                    'gpt-5.6-luna',
                    'v1',
                    '4444444444444444444444444444444444444444444444444444444444444444',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        long preferenceAiJobId;

        try (ResultSet aiJobResult = statement.executeQuery("""
                SELECT id
                FROM ai_jobs
                WHERE user_id = 2
                  AND idempotency_key = 'preference-ai-job-test-key'
                """)) {

            aiJobResult.next();
            preferenceAiJobId = aiJobResult.getLong("id");
        }

        statement.executeUpdate("""
                UPDATE preference_profiles
                SET ai_job_id = %d
                WHERE user_id = 2
                """.formatted(preferenceAiJobId));

        assertThatThrownBy(() -> statement.executeUpdate("""
                UPDATE preference_profiles
                SET ai_job_id = 999999999
                WHERE user_id = 2
                """))
                .isInstanceOf(SQLException.class);

        statement.executeUpdate("""
                DELETE FROM ai_jobs
                WHERE id = %d
                """.formatted(preferenceAiJobId));

        try (ResultSet aiJobNullResult = statement.executeQuery("""
                SELECT ai_job_id
                FROM preference_profiles
                WHERE user_id = 2
                """)) {

            aiJobNullResult.next();

            assertThat(aiJobNullResult.getObject("ai_job_id"))
                    .isNull();
        }

        statement.executeUpdate("""
                DELETE FROM users
                WHERE id = 2
                """);

        try (ResultSet preferenceCascadeResult = statement.executeQuery("""
                SELECT COUNT(*)
                FROM preference_profiles
                WHERE user_id = 2
                """)) {

            preferenceCascadeResult.next();

            assertThat(preferenceCascadeResult.getInt(1))
                    .isEqualTo(0);
        }
        }

    private void verifyWishlistConstraints(
            Statement statement,
            long productId
    ) throws SQLException {

        statement.executeUpdate("""
                    INSERT INTO users (
                        id,
                        nickname,
                        gender,
                        status,
                        created_at,
                        updated_at
                    ) VALUES (
                        3,
                        'wishlist-test-user',
                        'NOT_SPECIFIED',
                        'ACTIVE',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """);

        statement.executeUpdate("""
                    INSERT INTO wishlists (
                        user_id,
                        product_id,
                        created_at,
                        updated_at
                    ) VALUES (
                        3,
                        %d,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """.formatted(productId));

        assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO wishlists (
                        user_id,
                        product_id,
                        created_at,
                        updated_at
                    ) VALUES (
                        3,
                        %d,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """.formatted(productId)))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO wishlists (
                    user_id,
                    product_id,
                    created_at,
                    updated_at
                ) VALUES (
                    999999999,
                    %d,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """.formatted(productId)))
                .isInstanceOf(SQLException.class);

        assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO wishlists (
                    user_id,
                    product_id,
                    created_at,
                    updated_at
                ) VALUES (
                    3,
                    999999999,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                .isInstanceOf(SQLException.class);

        statement.executeUpdate("""
                DELETE FROM users
                WHERE id = 3
                """);

        try (ResultSet wishlistCascadeResult = statement.executeQuery("""
                SELECT COUNT(*)
                FROM wishlists
                WHERE user_id = 3
                """)) {

            wishlistCascadeResult.next();

            assertThat(wishlistCascadeResult.getInt(1))
                    .isEqualTo(0);
        }

        statement.executeUpdate("""
        INSERT INTO users (
            id,
            nickname,
            gender,
            status,
            created_at,
            updated_at
        ) VALUES (
              4,
              'wishlist-user-4',
              'NOT_SPECIFIED',
              'ACTIVE',
              CURRENT_TIMESTAMP(6),
              CURRENT_TIMESTAMP(6)
          )
        """);

        statement.executeUpdate("""
                INSERT INTO products (
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    'MCM',
                    'WISHLIST-PRODUCT-DELETE-TEST',
                    'Wishlist Product Delete Test',
                    'BAG',
                    100000,
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

        long wishlistProductId;

        try (ResultSet wishlistProductResult = statement.executeQuery("""
                SELECT id
                FROM products
                WHERE sku = 'WISHLIST-PRODUCT-DELETE-TEST'
                """)) {

            wishlistProductResult.next();
            wishlistProductId = wishlistProductResult.getLong("id");
        }

        statement.executeUpdate("""
                INSERT INTO wishlists (
                    user_id,
                    product_id,
                    created_at,
                    updated_at
                ) VALUES (
                    4,
                    %d,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
        """.formatted(wishlistProductId));

        assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM products
                WHERE id = %d
                """.formatted(wishlistProductId)))
                .isInstanceOf(SQLException.class);

        try (ResultSet productStillExistsResult = statement.executeQuery("""
                SELECT COUNT(*)
                FROM products
                WHERE id = %d
                """.formatted(wishlistProductId))) {

            productStillExistsResult.next();

            assertThat(productStillExistsResult.getInt(1))
                    .isEqualTo(1);
        }
    }
}