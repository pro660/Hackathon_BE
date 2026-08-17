package org.likelionhsu.hackathon.notification.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.notification.domain.NotificationData;
import org.likelionhsu.hackathon.notification.domain.NotificationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<NotificationData> findByUserId(
            Long userId,
            int limit,
            int offset,
            boolean ascending
    ) {
        String direction = ascending ? "ASC" : "DESC";

        String sql = """
                SELECT
                    id,
                    user_id,
                    type,
                    title,
                    message,
                    user_item_id,
                    item_name,
                    scheduled_date,
                    routine_types,
                    read_at,
                    created_at
                FROM notifications
                WHERE user_id = ?
                ORDER BY created_at %s, id %s
                LIMIT ? OFFSET ?
                """.formatted(direction, direction);

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> mapRow(resultSet),
                userId,
                limit,
                offset
        );
    }

    public long countByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM notifications
                WHERE user_id = ?
                """,
                Long.class,
                userId
        );

        return count == null ? 0L : count;
    }

    public Optional<NotificationData> findByIdAndUserId(
            Long notificationId,
            Long userId
    ) {
        List<NotificationData> rows =
                jdbcTemplate.query(
                        """
                        SELECT
                            id,
                            user_id,
                            type,
                            title,
                            message,
                            user_item_id,
                            item_name,
                            scheduled_date,
                            routine_types,
                            read_at,
                            created_at
                        FROM notifications
                        WHERE id = ?
                          AND user_id = ?
                        """,
                        (resultSet, rowNum) ->
                                mapRow(resultSet),
                        notificationId,
                        userId
                );

        return rows.stream().findFirst();
    }

    public int updateReadAt(
            Long notificationId,
            Long userId,
            Instant readAt,
            Instant now
    ) {
        return jdbcTemplate.update(
                """
                UPDATE notifications
                SET read_at = ?,
                    updated_at = ?
                WHERE id = ?
                  AND user_id = ?
                """,
                readAt == null
                        ? null
                        : Timestamp.from(readAt),
                Timestamp.from(now),
                notificationId,
                userId
        );
    }

    public void insertCareReminderIfAbsent(
            Long userId,
            String title,
            String message,
            Long userItemId,
            String itemName,
            java.time.LocalDate scheduledDate,
            List<CareRoutineType> routineTypes,
            String dedupKey,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO notifications (
                    user_id,
                    type,
                    title,
                    message,
                    user_item_id,
                    item_name,
                    scheduled_date,
                    routine_types,
                    dedup_key,
                    read_at,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    'CARE_REMINDER',
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    NULL,
                    ?,
                    ?
                )
                ON DUPLICATE KEY UPDATE
                    dedup_key = VALUES(dedup_key)
                """,
                userId,
                title,
                message,
                userItemId,
                itemName,
                Date.valueOf(scheduledDate),
                serializeRoutineTypes(routineTypes),
                dedupKey,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private NotificationData mapRow(
            ResultSet resultSet
    ) throws SQLException {
        return new NotificationData(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                NotificationType.valueOf(
                        resultSet.getString("type")
                ),
                resultSet.getString("title"),
                resultSet.getString("message"),
                nullableLong(
                        resultSet,
                        "user_item_id"
                ),
                resultSet.getString("item_name"),
                toLocalDate(
                        resultSet.getDate("scheduled_date")
                ),
                parseRoutineTypes(
                        resultSet.getString("routine_types")
                ),
                toInstant(resultSet.getTimestamp("read_at")),
                resultSet.getTimestamp("created_at")
                        .toInstant()
        );
    }

    private String serializeRoutineTypes(
            List<CareRoutineType> routineTypes
    ) {
        return String.join(
                ",",
                routineTypes
                        .stream()
                        .map(Enum::name)
                        .toList()
        );
    }

    private List<CareRoutineType> parseRoutineTypes(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(CareRoutineType::valueOf)
                .toList();
    }

    private Long nullableLong(
            ResultSet resultSet,
            String column
    ) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null
                ? null
                : timestamp.toInstant();
    }

    private java.time.LocalDate toLocalDate(Date date) {
        return date == null
                ? null
                : date.toLocalDate();
    }
}
