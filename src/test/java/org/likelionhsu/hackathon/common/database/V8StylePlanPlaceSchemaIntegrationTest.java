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
class V8StylePlanPlaceSchemaIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void duplicateProviderPlaceIsRejected() throws Exception {
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
                    INSERT INTO places (
                        id,
                        provider,
                        provider_place_id,
                        name,
                        latitude,
                        longitude,
                        created_at,
                        updated_at
                    ) VALUES (
                        400,
                        'KAKAO',
                        'kakao-place-400',
                        'V8 Test Place',
                        37.5665000,
                        126.9780000,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO places (
                        id,
                        provider,
                        provider_place_id,
                        name,
                        latitude,
                        longitude,
                        created_at,
                        updated_at
                    ) VALUES (
                        401,
                        'KAKAO',
                        'kakao-place-400',
                        'Duplicate V8 Test Place',
                        37.5670000,
                        126.9790000,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void duplicateSavedPlaceIsRejected() throws Exception {
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
                    401,
                    'v8-saved-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO places (
                    id,
                    provider,
                    provider_place_id,
                    name,
                    latitude,
                    longitude,
                    created_at,
                    updated_at
                ) VALUES (
                    402,
                    'KAKAO',
                    'kakao-place-402',
                    'V8 Saved Place',
                    37.5665000,
                    126.9780000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO saved_places (
                    user_id,
                    place_id,
                    created_at
                ) VALUES (
                    401,
                    402,
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO saved_places (
                    user_id,
                    place_id,
                    created_at
                ) VALUES (
                    401,
                    402,
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void savedPlacesAreDeletedWhenUserIsDeleted() throws Exception {
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
                    403,
                    'v8-cascade-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO places (
                    id,
                    provider,
                    provider_place_id,
                    name,
                    latitude,
                    longitude,
                    created_at,
                    updated_at
                ) VALUES (
                    403,
                    'KAKAO',
                    'kakao-place-403',
                    'V8 Cascade Place',
                    37.5665000,
                    126.9780000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO saved_places (
                    user_id,
                    place_id,
                    created_at
                ) VALUES (
                    403,
                    403,
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM users
                WHERE id = 403
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM saved_places
                WHERE user_id = 403
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM places
                WHERE id = 403
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void placeDeletionIsBlockedWhileSavedPlaceReferencesIt() throws Exception {
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
                    404,
                    'v8-place-restrict',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO places (
                    id,
                    provider,
                    provider_place_id,
                    name,
                    latitude,
                    longitude,
                    created_at,
                    updated_at
                ) VALUES (
                    404,
                    'KAKAO',
                    'kakao-place-404',
                    'V8 Restricted Place',
                    37.5665000,
                    126.9780000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO saved_places (
                    user_id,
                    place_id,
                    created_at
                ) VALUES (
                    404,
                    404,
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM places
                WHERE id = 404
                """))
                    .isInstanceOf(SQLException.class);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM places
                WHERE id = 404
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void stylePlanUserForeignKeyIsEnforced() throws Exception {
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
                INSERT INTO style_plans (
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    999999999,
                    'V8 Test Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void stylePlansAreDeletedWhenUserIsDeleted() throws Exception {
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
                    405,
                    'v8-style-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    405,
                    405,
                    'V8 Cascade Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM users
                WHERE id = 405
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM style_plans
                WHERE id = 405
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }
        }
    }

    @Test
    void stylePlanAiJobIsSetNullWhenAiJobIsDeleted() throws Exception {
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
                    406,
                    'v8-style-ai',
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
                    406,
                    406,
                    'STYLE_PLAN',
                    'COMPLETED',
                    'v8-style-ai-406',
                    'test-model',
                    'v1',
                    'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    ai_job_id,
                    created_at,
                    updated_at
                ) VALUES (
                    406,
                    406,
                    'V8 AI Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    406,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM ai_jobs
                WHERE id = 406
                """);

            try (var result = statement.executeQuery("""
                SELECT ai_job_id
                FROM style_plans
                WHERE id = 406
                """)) {

                result.next();

                assertThat(result.getObject("ai_job_id"))
                        .isNull();
            }

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM style_plans
                WHERE id = 406
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void stylePlanAiJobForeignKeyIsEnforced() throws Exception {
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
                    407,
                    'v8-style-ai-fk',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plans (
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    ai_job_id,
                    created_at,
                    updated_at
                ) VALUES (
                    407,
                    'V8 Invalid AI Job Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    999999999,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void duplicateStylePlanAiJobIsRejected() throws Exception {
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
                    408,
                    'v8-style-unique',
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
                    408,
                    408,
                    'STYLE_PLAN',
                    'COMPLETED',
                    'v8-style-unique-408',
                    'test-model',
                    'v1',
                    'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    ai_job_id,
                    created_at,
                    updated_at
                ) VALUES (
                    408,
                    408,
                    'V8 Style Plan One',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    408,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    ai_job_id,
                    created_at,
                    updated_at
                ) VALUES (
                    409,
                    408,
                    'V8 Style Plan Two',
                    'DATE',
                    'AI',
                    'DRAFT',
                    408,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void duplicateStylePlanItemIsRejected() throws Exception {
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
                    409,
                    'v8-plan-item-user',
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
                    409,
                    409,
                    'V8 Style Plan Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    409,
                    409,
                    'V8 Item Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_items (
                    style_plan_id,
                    user_item_id,
                    role,
                    sort_order
                ) VALUES (
                    409,
                    409,
                    'BAG',
                    0
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_items (
                    style_plan_id,
                    user_item_id,
                    role,
                    sort_order
                ) VALUES (
                    409,
                    409,
                    'MAIN',
                    1
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void negativeStylePlanItemSortOrderIsRejected() throws Exception {
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
                    410,
                    'v8-item-sort',
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
                    410,
                    410,
                    'V8 Sort Test Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    410,
                    410,
                    'V8 Sort Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_items (
                    style_plan_id,
                    user_item_id,
                    role,
                    sort_order
                ) VALUES (
                    410,
                    410,
                    'BAG',
                    -1
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void stylePlanItemForeignKeysAreEnforced() throws Exception {
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
                    411,
                    'v8-item-fk-user',
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
                    411,
                    411,
                    'V8 Item FK Test',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    411,
                    411,
                    'V8 Item FK Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_items (
                    style_plan_id,
                    user_item_id,
                    role,
                    sort_order
                ) VALUES (
                    999999999,
                    411,
                    'BAG',
                    0
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_items (
                    style_plan_id,
                    user_item_id,
                    role,
                    sort_order
                ) VALUES (
                    411,
                    999999999,
                    'BAG',
                    0
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void stylePlanItemsAreDeletedWhenStylePlanIsDeleted() throws Exception {
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
                    412,
                    'v8-item-cascade',
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
                    412,
                    412,
                    'V8 Cascade Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    412,
                    412,
                    'V8 Cascade Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_items (
                    id,
                    style_plan_id,
                    user_item_id,
                    role,
                    sort_order
                ) VALUES (
                    412,
                    412,
                    412,
                    'BAG',
                    0
                )
                """);

            statement.executeUpdate("""
                DELETE FROM style_plans
                WHERE id = 412
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM style_plan_items
                WHERE id = 412
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM user_items
                WHERE id = 412
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void userItemDeletionIsBlockedWhileStylePlanItemReferencesIt() throws Exception {
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
                    413,
                    'v8-item-restrict',
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
                    413,
                    413,
                    'V8 Restricted Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    413,
                    413,
                    'V8 Restricted Item Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_items (
                    style_plan_id,
                    user_item_id,
                    role,
                    sort_order
                ) VALUES (
                    413,
                    413,
                    'BAG',
                    0
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM user_items
                WHERE id = 413
                """))
                    .isInstanceOf(SQLException.class);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM user_items
                WHERE id = 413
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void duplicateStylePlanProductIsRejected() throws Exception {
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
                    414,
                    'v8-plan-product',
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
                    414,
                    'MCM',
                    'V8-PLAN-PRODUCT-414',
                    'V8 Style Plan Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v8-plan-product-414',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    414,
                    414,
                    'V8 Product Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_products (
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                ) VALUES (
                    414,
                    414,
                    1,
                    'First recommendation'
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_products (
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                ) VALUES (
                    414,
                    414,
                    2,
                    'Duplicate product'
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void duplicateStylePlanProductRankIsRejected() throws Exception {
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
                    415,
                    'v8-product-rank',
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
                    415,
                    'MCM',
                    'V8-RANK-415',
                    'V8 Rank Product One',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v8-rank-415',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                ),
                (
                    416,
                    'MCM',
                    'V8-RANK-416',
                    'V8 Rank Product Two',
                    'BAG',
                    1100000,
                    'BROWN',
                    'LEATHER',
                    'https://example.com/v8-rank-416',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    415,
                    415,
                    'V8 Rank Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_products (
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                ) VALUES (
                    415,
                    415,
                    1,
                    'First ranked product'
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_products (
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                ) VALUES (
                    415,
                    416,
                    1,
                    'Second product with duplicate rank'
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void invalidStylePlanProductRankIsRejected() throws Exception {
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
                    417,
                    'v8-rank-check',
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
                    417,
                    'MCM',
                    'V8-RANK-CHECK-417',
                    'V8 Rank Check Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v8-rank-check-417',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    417,
                    417,
                    'V8 Rank Check Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_products (
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                ) VALUES (
                    417,
                    417,
                    0,
                    'Invalid rank'
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void stylePlanProductForeignKeysAreEnforced() throws Exception {
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
                    418,
                    'v8-product-fk',
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
                    418,
                    'MCM',
                    'V8-PRODUCT-FK-418',
                    'V8 Product FK Test',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v8-product-fk-418',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    418,
                    418,
                    'V8 Product FK Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_products (
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                ) VALUES (
                    999999999,
                    418,
                    1,
                    'Invalid style plan'
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_products (
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                ) VALUES (
                    418,
                    999999999,
                    1,
                    'Invalid product'
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void stylePlanProductsAreDeletedWhenStylePlanIsDeleted() throws Exception {
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
                    419,
                    'v8-product-cascade',
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
                    419,
                    'MCM',
                    'V8-PRODUCT-CASCADE-419',
                    'V8 Product Cascade Test',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v8-product-cascade-419',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    419,
                    419,
                    'V8 Product Cascade Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_products (
                    id,
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                ) VALUES (
                    419,
                    419,
                    419,
                    1,
                    'Cascade test'
                )
                """);

            statement.executeUpdate("""
                DELETE FROM style_plans
                WHERE id = 419
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM style_plan_products
                WHERE id = 419
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM products
                WHERE id = 419
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void productDeletionIsBlockedWhileStylePlanProductReferencesIt() throws Exception {
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
                    420,
                    'v8-product-restrict',
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
                    420,
                    'MCM',
                    'V8-PRODUCT-RESTRICT-420',
                    'V8 Restricted Product',
                    'BAG',
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/v8-product-restrict-420',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    420,
                    420,
                    'V8 Restricted Product Style Plan',
                    'DAILY',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_products (
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                ) VALUES (
                    420,
                    420,
                    1,
                    'Restrict test'
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM products
                WHERE id = 420
                """))
                    .isInstanceOf(SQLException.class);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM products
                WHERE id = 420
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void duplicateStylePlanPlaceIsRejected() throws Exception {
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
                    421,
                    'v8-plan-place',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO places (
                    id,
                    provider,
                    provider_place_id,
                    name,
                    latitude,
                    longitude,
                    created_at,
                    updated_at
                ) VALUES (
                    421,
                    'KAKAO',
                    'kakao-place-421',
                    'V8 Style Plan Place',
                    37.5665000,
                    126.9780000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    421,
                    421,
                    'V8 Place Style Plan',
                    'DATE',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_places (
                    style_plan_id,
                    place_id,
                    rank_order,
                    reason
                ) VALUES (
                    421,
                    421,
                    1,
                    'First place recommendation'
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_places (
                    style_plan_id,
                    place_id,
                    rank_order,
                    reason
                ) VALUES (
                    421,
                    421,
                    2,
                    'Duplicate place'
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void duplicateStylePlanPlaceRankIsRejected() throws Exception {
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
                    422,
                    'v8-place-rank',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO places (
                    id,
                    provider,
                    provider_place_id,
                    name,
                    latitude,
                    longitude,
                    created_at,
                    updated_at
                ) VALUES
                (
                    422,
                    'KAKAO',
                    'kakao-place-422',
                    'V8 Rank Place One',
                    37.5665000,
                    126.9780000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                ),
                (
                    423,
                    'KAKAO',
                    'kakao-place-423',
                    'V8 Rank Place Two',
                    37.5670000,
                    126.9790000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    422,
                    422,
                    'V8 Place Rank Style Plan',
                    'DATE',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_places (
                    style_plan_id,
                    place_id,
                    rank_order,
                    reason
                ) VALUES (
                    422,
                    422,
                    1,
                    'First ranked place'
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_places (
                    style_plan_id,
                    place_id,
                    rank_order,
                    reason
                ) VALUES (
                    422,
                    423,
                    1,
                    'Second place with duplicate rank'
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void invalidStylePlanPlaceRankIsRejected() throws Exception {
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
                    424,
                    'v8-place-check',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO places (
                    id,
                    provider,
                    provider_place_id,
                    name,
                    latitude,
                    longitude,
                    created_at,
                    updated_at
                ) VALUES (
                    424,
                    'KAKAO',
                    'kakao-place-424',
                    'V8 Place Rank Check',
                    37.5665000,
                    126.9780000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    424,
                    424,
                    'V8 Place Rank Check Style Plan',
                    'DATE',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_places (
                    style_plan_id,
                    place_id,
                    rank_order,
                    reason
                ) VALUES (
                    424,
                    424,
                    0,
                    'Invalid rank'
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void stylePlanPlaceForeignKeysAreEnforced() throws Exception {
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
                    425,
                    'v8-place-fk',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO places (
                    id,
                    provider,
                    provider_place_id,
                    name,
                    latitude,
                    longitude,
                    created_at,
                    updated_at
                ) VALUES (
                    425,
                    'KAKAO',
                    'kakao-place-425',
                    'V8 Place FK Test',
                    37.5665000,
                    126.9780000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    425,
                    425,
                    'V8 Place FK Style Plan',
                    'DATE',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_places (
                    style_plan_id,
                    place_id,
                    rank_order,
                    reason
                ) VALUES (
                    999999999,
                    425,
                    1,
                    'Invalid style plan'
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO style_plan_places (
                    style_plan_id,
                    place_id,
                    rank_order,
                    reason
                ) VALUES (
                    425,
                    999999999,
                    1,
                    'Invalid place'
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void stylePlanPlacesAreDeletedWhenStylePlanIsDeleted() throws Exception {
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
                    426,
                    'v8-place-cascade',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO places (
                    id,
                    provider,
                    provider_place_id,
                    name,
                    latitude,
                    longitude,
                    created_at,
                    updated_at
                ) VALUES (
                    426,
                    'KAKAO',
                    'kakao-place-426',
                    'V8 Place Cascade Test',
                    37.5665000,
                    126.9780000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    426,
                    426,
                    'V8 Place Cascade Style Plan',
                    'DATE',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_places (
                    id,
                    style_plan_id,
                    place_id,
                    rank_order,
                    reason
                ) VALUES (
                    426,
                    426,
                    426,
                    1,
                    'Cascade test'
                )
                """);

            statement.executeUpdate("""
                DELETE FROM style_plans
                WHERE id = 426
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM style_plan_places
                WHERE id = 426
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM places
                WHERE id = 426
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void placeDeletionIsBlockedWhileStylePlanPlaceReferencesIt() throws Exception {
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
                    427,
                    'v8-place-restrict',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO places (
                    id,
                    provider,
                    provider_place_id,
                    name,
                    latitude,
                    longitude,
                    created_at,
                    updated_at
                ) VALUES (
                    427,
                    'KAKAO',
                    'kakao-place-427',
                    'V8 Restricted Style Plan Place',
                    37.5665000,
                    126.9780000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plans (
                    id,
                    user_id,
                    title,
                    occasion,
                    generation_type,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    427,
                    427,
                    'V8 Restricted Place Style Plan',
                    'DATE',
                    'AI',
                    'DRAFT',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO style_plan_places (
                    style_plan_id,
                    place_id,
                    rank_order,
                    reason
                ) VALUES (
                    427,
                    427,
                    1,
                    'Restrict test'
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM places
                WHERE id = 427
                """))
                    .isInstanceOf(SQLException.class);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM places
                WHERE id = 427
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