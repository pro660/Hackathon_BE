package org.likelionhsu.hackathon.careguide.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.likelionhsu.hackathon.careguide.domain.CareReminderSetting;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CareReminderSettingRepository {

    private final JdbcTemplate jdbcTemplate;

    public CareReminderSettingRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CareReminderSetting> findByUserItemId(
            Long userItemId
    ) {
        List<CareReminderSetting> rows =
                jdbcTemplate.query(
                        """
                        SELECT
                            user_item_id,
                            enabled,
                            enabled_at
                        FROM user_item_care_reminder_settings
                        WHERE user_item_id = ?
                        """,
                        (resultSet, rowNum) ->
                                new CareReminderSetting(
                                        resultSet.getLong(
                                                "user_item_id"
                                        ),
                                        resultSet.getBoolean(
                                                "enabled"
                                        ),
                                        toInstant(
                                                resultSet.getTimestamp(
                                                        "enabled_at"
                                                )
                                        )
                                ),
                        userItemId
                );

        return rows.stream().findFirst();
    }

    public List<CareReminderSetting> findAllEnabled() {
        return jdbcTemplate.query(
                """
                SELECT
                    user_item_id,
                    enabled,
                    enabled_at
                FROM user_item_care_reminder_settings
                WHERE enabled = TRUE
                ORDER BY user_item_id ASC
                """,
                (resultSet, rowNum) ->
                        new CareReminderSetting(
                                resultSet.getLong(
                                        "user_item_id"
                                ),
                                true,
                                toInstant(
                                        resultSet.getTimestamp(
                                                "enabled_at"
                                        )
                                )
                        )
        );
    }

    public void upsert(
            Long userItemId,
            boolean enabled,
            Instant enabledAt,
            Instant now
    ) {
        Timestamp enabledTimestamp =
                enabledAt == null
                        ? null
                        : Timestamp.from(enabledAt);
        Timestamp nowTimestamp =
                Timestamp.from(now);

        jdbcTemplate.update(
                """
                INSERT INTO user_item_care_reminder_settings (
                    user_item_id,
                    enabled,
                    enabled_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    enabled = VALUES(enabled),
                    enabled_at = VALUES(enabled_at),
                    updated_at = VALUES(updated_at)
                """,
                userItemId,
                enabled,
                enabledTimestamp,
                nowTimestamp,
                nowTimestamp
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null
                ? null
                : timestamp.toInstant();
    }
}
