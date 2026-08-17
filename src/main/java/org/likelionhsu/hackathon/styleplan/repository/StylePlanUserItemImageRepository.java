package org.likelionhsu.hackathon.styleplan.repository;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StylePlanUserItemImageRepository {

    private final JdbcTemplate jdbcTemplate;

    public StylePlanUserItemImageRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> findPrimaryImageUrl(
            Long userId,
            Long userItemId
    ) {
        return jdbcTemplate.query(
                """
                SELECT secure_url
                FROM image_assets
                WHERE owner_user_id = ?
                  AND user_item_id = ?
                  AND purpose = 'ITEM'
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                ORDER BY sort_order ASC, id ASC
                LIMIT 1
                """,
                (resultSet, rowNumber) ->
                        resultSet.getString("secure_url"),
                userId,
                userItemId
        ).stream().findFirst();
    }
}
