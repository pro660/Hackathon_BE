package org.likelionhsu.hackathon.styleplan.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

import org.likelionhsu.hackathon.styleplan.domain.StyleItemRole;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanWeatherCondition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class StylePlanPersistenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public StylePlanPersistenceRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByAiJobId(Long aiJobId) {
        if (aiJobId == null) {
            return false;
        }

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM style_plans
                WHERE ai_job_id = ?
                """,
                Integer.class,
                aiJobId
        );

        return count != null && count > 0;
    }

    public long insertPlan(
            Long userId,
            String title,
            StylePlanOccasion occasion,
            Instant plannedAt,
            StylePlanWeatherCondition weatherCondition,
            String description,
            StylePlanGenerationType generationType,
            StylePlanStatus status,
            Long aiJobId
    ) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(occasion, "occasion");
        Objects.requireNonNull(
                generationType,
                "generationType"
        );
        Objects.requireNonNull(status, "status");

        KeyHolder keyHolder =
                new GeneratedKeyHolder();

        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    """
                                    INSERT INTO style_plans (
                                        user_id,
                                        title,
                                        occasion,
                                        planned_at,
                                        weather_summary,
                                        weather_condition,
                                        description,
                                        generation_type,
                                        status,
                                        ai_job_id,
                                        version,
                                        created_at,
                                        updated_at
                                    )
                                    VALUES (
                                        ?, ?, ?, ?, NULL, ?, ?, ?, ?, ?,
                                        0,
                                        CURRENT_TIMESTAMP(6),
                                        CURRENT_TIMESTAMP(6)
                                    )
                                    """,
                                    Statement.RETURN_GENERATED_KEYS
                            );

                    statement.setLong(1, userId);
                    statement.setString(2, title);
                    statement.setString(
                            3,
                            occasion.name()
                    );

                    if (plannedAt == null) {
                        statement.setTimestamp(4, null);
                    } else {
                        statement.setTimestamp(
                                4,
                                Timestamp.from(plannedAt)
                        );
                    }

                    statement.setString(
                            5,
                            weatherCondition == null
                                    ? null
                                    : weatherCondition.name()
                    );
                    statement.setString(
                            6,
                            description
                    );
                    statement.setString(
                            7,
                            generationType.name()
                    );
                    statement.setString(
                            8,
                            status.name()
                    );

                    if (aiJobId == null) {
                        statement.setObject(9, null);
                    } else {
                        statement.setLong(9, aiJobId);
                    }

                    return statement;
                },
                keyHolder
        );

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException(
                    "생성된 StylePlan ID를 가져오지 못했습니다."
            );
        }

        return key.longValue();
    }

    public void insertItem(
            Long stylePlanId,
            Long userItemId,
            StyleItemRole role,
            int sortOrder
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO style_plan_items (
                    style_plan_id,
                    user_item_id,
                    role,
                    sort_order
                )
                VALUES (?, ?, ?, ?)
                """,
                stylePlanId,
                userItemId,
                role.name(),
                sortOrder
        );
    }

    public void insertProduct(
            Long stylePlanId,
            Long productId,
            int rank,
            String reason
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO style_plan_products (
                    style_plan_id,
                    product_id,
                    rank_order,
                    reason
                )
                VALUES (?, ?, ?, ?)
                """,
                stylePlanId,
                productId,
                rank,
                reason
        );
    }
}
