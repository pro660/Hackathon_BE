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
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Tag("integration")
class V5UserItemImageSchemaIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void negativePurchasePriceIsRejected() throws Exception {
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
                        100,
                        'v5-test-user',
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
                        status,
                        created_at,
                        updated_at
                    ) VALUES (
                        100,
                        'MCM',
                        'V5-TEST-PRODUCT',
                        'V5 Test Product',
                        'BAG',
                        100000,
                        'ACTIVE',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO user_items (
                        user_id,
                        product_id,
                        name,
                        category,
                        purchase_price,
                        status,
                        created_at,
                        updated_at
                    ) VALUES (
                        100,
                        100,
                        'V5 Price Test Item',
                        'BAG',
                        -1,
                        'ACTIVE',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }
    @Test
    void userItemForeignKeysAreEnforced() throws Exception {
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
                    101,
                    'v5-fk-user',
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
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    101,
                    'MCM',
                    'V5-FK-PRODUCT',
                    'V5 FK Product',
                    'BAG',
                    100000,
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO user_items (
                    user_id,
                    product_id,
                    name,
                    category,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    999999999,
                    101,
                    'Invalid User Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO user_items (
                    user_id,
                    product_id,
                    name,
                    category,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    101,
                    999999999,
                    'Invalid Product Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }
    @Test
    void userItemAiJobForeignKeyAndSetNullWork() throws Exception {
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
                    102,
                    'v5-ai-user',
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
                    102,
                    102,
                    'ITEM_ANALYSIS',
                    'SUCCEEDED',
                    'v5-user-item-ai-job',
                    'gpt-5.6-luna',
                    'v1',
                    '5555555555555555555555555555555555555555555555555555555555555555',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO user_items (
                    user_id,
                    name,
                    category,
                    status,
                    ai_job_id,
                    created_at,
                    updated_at
                ) VALUES (
                    102,
                    'AI Linked Item',
                    'BAG',
                    'ACTIVE',
                    102,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                UPDATE user_items
                SET ai_job_id = 999999999
                WHERE user_id = 102
                  AND name = 'AI Linked Item'
                """))
                    .isInstanceOf(SQLException.class);

            statement.executeUpdate("""
                DELETE FROM ai_jobs
                WHERE id = 102
                """);

            try (ResultSet result = statement.executeQuery("""
                SELECT ai_job_id
                FROM user_items
                WHERE user_id = 102
                  AND name = 'AI Linked Item'
                """)) {

                result.next();

                assertThat(result.getObject("ai_job_id"))
                        .isNull();
            }
        }
    }

    @Test
    void userItemIsDeletedWhenUserIsDeleted() throws Exception {
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
                    103,
                    'v5-cascade-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO user_items (
                    user_id,
                    name,
                    category,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    103,
                    'Cascade Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM users
                WHERE id = 103
                """);

            try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM user_items
                WHERE user_id = 103
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }
        }
    }

    @Test
    void imageAssetPublicIdIsUnique() throws Exception {
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
                    104,
                    'v5-image-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    104,
                    'PROFILE',
                    'v5-image-public-id',
                    'https://example.com/image-1.jpg',
                    'jpg',
                    1000,
                    100,
                    100,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    104,
                    'PROFILE',
                    'v5-image-public-id',
                    'https://example.com/image-2.jpg',
                    'png',
                    2000,
                    200,
                    200,
                    'ACTIVE',
                    1,
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void imageAssetNumericChecksAreEnforced() throws Exception {
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
                    105,
                    'v5-check-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    105,
                    'PROFILE',
                    'v5-negative-sort-order',
                    'https://example.com/negative-sort.jpg',
                    'jpg',
                    1000,
                    100,
                    100,
                    'ACTIVE',
                    -1,
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    105,
                    'PROFILE',
                    'v5-zero-bytes',
                    'https://example.com/zero-bytes.jpg',
                    'jpg',
                    0,
                    100,
                    100,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    105,
                    'PROFILE',
                    'v5-zero-width',
                    'https://example.com/zero-width.jpg',
                    'jpg',
                    1000,
                    0,
                    100,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    105,
                    'PROFILE',
                    'v5-zero-height',
                    'https://example.com/zero-height.jpg',
                    'jpg',
                    1000,
                    100,
                    0,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void imageAssetForeignKeysAreEnforced() throws Exception {
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
                    106,
                    'v5-image-fk-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO user_items (
                    id,
                    user_id,
                    name,
                    category,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    106,
                    106,
                    'V5 Image FK Item',
                    'BAG',
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
                    106,
                    106,
                    'ITEM_ANALYSIS',
                    'SUCCEEDED',
                    'v5-image-fk-ai-job',
                    'gpt-5.6-luna',
                    'v1',
                    '6666666666666666666666666666666666666666666666666666666666666666',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    999999999,
                    'PROFILE',
                    'v5-invalid-owner-user',
                    'https://example.com/invalid-owner.jpg',
                    'jpg',
                    1000,
                    100,
                    100,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    user_item_id,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    106,
                    'ITEM',
                    999999999,
                    'v5-invalid-user-item',
                    'https://example.com/invalid-item.jpg',
                    'jpg',
                    1000,
                    100,
                    100,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    ai_job_id,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    106,
                    'AI_INPUT',
                    999999999,
                    'v5-invalid-ai-job',
                    'https://example.com/invalid-ai-job.jpg',
                    'jpg',
                    1000,
                    100,
                    100,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void imageAssetAiJobIsSetNullWhenAiJobIsDeleted() throws Exception {
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
                    107,
                    'v5-setnull-user',
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
                    107,
                    107,
                    'ITEM_ANALYSIS',
                    'SUCCEEDED',
                    'v5-image-set-null-ai-job',
                    'gpt-5.6-luna',
                    'v1',
                    '7777777777777777777777777777777777777777777777777777777777777777',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    ai_job_id,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    107,
                    'AI_INPUT',
                    107,
                    'v5-image-set-null-test',
                    'https://example.com/set-null-test.jpg',
                    'jpg',
                    1000,
                    100,
                    100,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM ai_jobs
                WHERE id = 107
                """);

            try (ResultSet result = statement.executeQuery("""
                SELECT ai_job_id
                FROM image_assets
                WHERE public_id = 'v5-image-set-null-test'
                """)) {

                result.next();

                assertThat(result.getObject("ai_job_id"))
                        .isNull();
            }
        }
    }

    @Test
    void imageAssetIsDeletedWhenOwnerUserIsDeleted() throws Exception {
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
                    108,
                    'v5-image-owner',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    108,
                    'PROFILE',
                    'v5-owner-cascade-test',
                    'https://example.com/owner-cascade.jpg',
                    'jpg',
                    1000,
                    100,
                    100,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM users
                WHERE id = 108
                """);

            try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM image_assets
                WHERE owner_user_id = 108
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }
        }
    }

    @Test
    void userItemDeletionIsBlockedWhileImageAssetReferencesIt() throws Exception {
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
                    109,
                    'v5-item-image',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO user_items (
                    id,
                    user_id,
                    name,
                    category,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    109,
                    109,
                    'Referenced User Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO image_assets (
                    owner_user_id,
                    purpose,
                    user_item_id,
                    public_id,
                    secure_url,
                    format,
                    bytes,
                    width,
                    height,
                    status,
                    sort_order,
                    created_at
                ) VALUES (
                    109,
                    'ITEM',
                    109,
                    'v5-user-item-delete-block-test',
                    'https://example.com/item-delete-block.jpg',
                    'jpg',
                    1000,
                    100,
                    100,
                    'ACTIVE',
                    0,
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM user_items
                WHERE id = 109
                """))
                    .isInstanceOf(SQLException.class);

            try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM user_items
                WHERE id = 109
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