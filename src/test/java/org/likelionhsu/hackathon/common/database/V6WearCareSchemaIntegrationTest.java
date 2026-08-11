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
class V6WearCareSchemaIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void wearRecordUserForeignKeyIsEnforced() throws Exception {
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
                    INSERT INTO wear_records (
                        user_id,
                        worn_at,
                        occasion,
                        created_at,
                        updated_at
                    ) VALUES (
                        999999999,
                        CURRENT_TIMESTAMP(6),
                        'DAILY',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void wearRecordsAreDeletedWhenUserIsDeleted() throws Exception {
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
                    200,
                    'v6-wear-user',
                    'NOT_SPECIFIED',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO wear_records (
                    user_id,
                    worn_at,
                    occasion,
                    created_at,
                    updated_at
                ) VALUES (
                    200,
                    CURRENT_TIMESTAMP(6),
                    'DAILY',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                DELETE FROM users
                WHERE id = 200
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM wear_records
                WHERE user_id = 200
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }
        }
    }

    @Test
    void duplicateWearRecordItemIsRejected() throws Exception {
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
                    201,
                    'v6-item-user',
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
                    201,
                    201,
                    'V6 Wear Test Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO wear_records (
                    id,
                    user_id,
                    worn_at,
                    occasion,
                    created_at,
                    updated_at
                ) VALUES (
                    201,
                    201,
                    CURRENT_TIMESTAMP(6),
                    'DAILY',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO wear_record_items (
                    wear_record_id,
                    user_item_id,
                    sort_order
                ) VALUES (
                    201,
                    201,
                    0
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO wear_record_items (
                    wear_record_id,
                    user_item_id,
                    sort_order
                ) VALUES (
                    201,
                    201,
                    1
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void negativeWearRecordItemSortOrderIsRejected() throws Exception {
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
                    202,
                    'v6-sort-user',
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
                    202,
                    202,
                    'V6 Sort Test Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO wear_records (
                    id,
                    user_id,
                    worn_at,
                    occasion,
                    created_at,
                    updated_at
                ) VALUES (
                    202,
                    202,
                    CURRENT_TIMESTAMP(6),
                    'DAILY',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO wear_record_items (
                    wear_record_id,
                    user_item_id,
                    sort_order
                ) VALUES (
                    202,
                    202,
                    -1
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void wearRecordItemForeignKeysAreEnforced() throws Exception {
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
                    203,
                    'v6-fk-user',
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
                    203,
                    203,
                    'V6 FK Test Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO wear_records (
                    id,
                    user_id,
                    worn_at,
                    occasion,
                    created_at,
                    updated_at
                ) VALUES (
                    203,
                    203,
                    CURRENT_TIMESTAMP(6),
                    'DAILY',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO wear_record_items (
                    wear_record_id,
                    user_item_id,
                    sort_order
                ) VALUES (
                    999999999,
                    203,
                    0
                )
                """))
                    .isInstanceOf(SQLException.class);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO wear_record_items (
                    wear_record_id,
                    user_item_id,
                    sort_order
                ) VALUES (
                    203,
                    999999999,
                    0
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void wearRecordItemsAreDeletedWhenWearRecordIsDeleted() throws Exception {
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
                    204,
                    'v6-cascade-user',
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
                    204,
                    204,
                    'V6 Cascade Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO wear_records (
                    id,
                    user_id,
                    worn_at,
                    occasion,
                    created_at,
                    updated_at
                ) VALUES (
                    204,
                    204,
                    CURRENT_TIMESTAMP(6),
                    'DAILY',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO wear_record_items (
                    wear_record_id,
                    user_item_id,
                    sort_order
                ) VALUES (
                    204,
                    204,
                    0
                )
                """);

            statement.executeUpdate("""
                DELETE FROM wear_records
                WHERE id = 204
                """);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM wear_record_items
                WHERE wear_record_id = 204
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(0);
            }

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM user_items
                WHERE id = 204
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void userItemDeletionIsBlockedWhileWearRecordItemReferencesIt() throws Exception {
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
                    205,
                    'v6-restrict-user',
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
                    205,
                    205,
                    'V6 Restricted Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO wear_records (
                    id,
                    user_id,
                    worn_at,
                    occasion,
                    created_at,
                    updated_at
                ) VALUES (
                    205,
                    205,
                    CURRENT_TIMESTAMP(6),
                    'DAILY',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO wear_record_items (
                    wear_record_id,
                    user_item_id,
                    sort_order
                ) VALUES (
                    205,
                    205,
                    0
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM user_items
                WHERE id = 205
                """))
                    .isInstanceOf(SQLException.class);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM user_items
                WHERE id = 205
                """)) {

                result.next();

                assertThat(result.getInt(1))
                        .isEqualTo(1);
            }
        }
    }

    @Test
    void negativeCareRecordCostIsRejected() throws Exception {
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
                    206,
                    'v6-care-user',
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
                    206,
                    206,
                    'V6 Care Test Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO care_records (
                    user_item_id,
                    care_type,
                    cared_at,
                    cost,
                    created_at,
                    updated_at
                ) VALUES (
                    206,
                    'CLEANING',
                    CURRENT_TIMESTAMP(6),
                    -1,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void careRecordUserItemForeignKeyIsEnforced() throws Exception {
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
                INSERT INTO care_records (
                    user_item_id,
                    care_type,
                    cared_at,
                    created_at,
                    updated_at
                ) VALUES (
                    999999999,
                    'CLEANING',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void userItemDeletionIsBlockedWhileCareRecordReferencesIt() throws Exception {
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
                    207,
                    'v6-care-restrict',
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
                    207,
                    207,
                    'V6 Care Restricted Item',
                    'BAG',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            statement.executeUpdate("""
                INSERT INTO care_records (
                    user_item_id,
                    care_type,
                    cared_at,
                    cost,
                    created_at,
                    updated_at
                ) VALUES (
                    207,
                    'CLEANING',
                    CURRENT_TIMESTAMP(6),
                    10000,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                DELETE FROM user_items
                WHERE id = 207
                """))
                    .isInstanceOf(SQLException.class);

            try (var result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM user_items
                WHERE id = 207
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