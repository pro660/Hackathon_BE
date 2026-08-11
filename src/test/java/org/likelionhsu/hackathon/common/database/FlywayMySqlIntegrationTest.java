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
            ResultSet tableResult = statement.executeQuery("""
                    SELECT COUNT(*)
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
                          'product_tag_mappings'
                      )
                    """);

            tableResult.next();
            assertThat(tableResult.getInt(1)).isEqualTo(12);

            ResultSet historyResult = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history
                    WHERE version IN ('1', '2', '3')
                      AND success = 1
                    """);

            historyResult.next();
            assertThat(historyResult.getInt(1)).isEqualTo(3);

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

            ResultSet jsonResult = statement.executeQuery("""
                    SELECT
                        JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.title')) AS title,
                        JSON_EXTRACT(result_json, '$.score') AS score,
                        JSON_UNQUOTE(JSON_EXTRACT(fallback_json, '$.reason')) AS fallback_reason
                    FROM ai_jobs
                    WHERE user_id = 1
                      AND idempotency_key = 'json-test-key'
                    """);

            jsonResult.next();

            assertThat(jsonResult.getString("title"))
                    .isEqualTo("오늘의 스타일");

            assertThat(jsonResult.getInt("score"))
                    .isEqualTo(95);

            assertThat(jsonResult.getString("fallback_reason"))
                    .isEqualTo("fallback-test");

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

            ResultSet retryDefaultResult = statement.executeQuery("""
                    SELECT retry_count
                    FROM ai_jobs
                    WHERE user_id = 1
                      AND idempotency_key = 'retry-default-test-key'
                    """);

            retryDefaultResult.next();
            assertThat(retryDefaultResult.getInt("retry_count"))
                    .isEqualTo(0);

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

            ResultSet productResult = statement.executeQuery("""
                    SELECT id
                    FROM products
                    WHERE sku = 'SKU-UNIQUE-TEST'
                    """);

            productResult.next();
            long productId = productResult.getLong("id");

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

            ResultSet cascadeProductResult = statement.executeQuery("""
                    SELECT id
                    FROM products
                    WHERE sku = 'CASCADE-IMAGE-TEST'
                    """);

            cascadeProductResult.next();
            long cascadeProductId = cascadeProductResult.getLong("id");

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

            ResultSet cascadeImageResult = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM product_images
                    WHERE product_id = %d
                    """.formatted(cascadeProductId));

            cascadeImageResult.next();
            assertThat(cascadeImageResult.getInt(1)).isEqualTo(0);

            statement.executeUpdate("""
                    INSERT INTO product_tags (
                        type,
                        code,
                        display_name
                    ) VALUES (
                        'STYLE',
                        'CASUAL',
                        '캐주얼'
                    )
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO product_tags (
                        type,
                        code,
                        display_name
                    ) VALUES (
                        'STYLE',
                        'CASUAL',
                        '다른 캐주얼 이름'
                    )
                    """))
                    .isInstanceOf(SQLException.class);

            ResultSet productTagResult = statement.executeQuery("""
                    SELECT id
                    FROM product_tags
                    WHERE type = 'STYLE'
                      AND code = 'CASUAL'
                    """);

            productTagResult.next();
            long productTagId = productTagResult.getLong("id");

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

            ResultSet cascadeTagProductResult = statement.executeQuery("""
        SELECT id
        FROM products
        WHERE sku = 'CASCADE-TAG-MAPPING-TEST'
        """);

            cascadeTagProductResult.next();
            long cascadeTagProductId = cascadeTagProductResult.getLong("id");

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

            ResultSet cascadeTagMappingResult = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM product_tag_mappings
                    WHERE product_id = %d
                    """.formatted(cascadeTagProductId));

            cascadeTagMappingResult.next();
            assertThat(cascadeTagMappingResult.getInt(1))
                    .isEqualTo(0);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    DELETE FROM product_tags
                    WHERE id = %d
                    """.formatted(productTagId)))
                    .isInstanceOf(SQLException.class);
        }
    }
}