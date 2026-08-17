package org.likelionhsu.hackathon.place.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class StylePlanPlaceRepository {

    private final JdbcTemplate jdbcTemplate;

    public StylePlanPlaceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void replace(
            long stylePlanId,
            List<StylePlanPlaceLink> links
    ) {
        jdbcTemplate.update(
                "DELETE FROM style_plan_places WHERE style_plan_id = ?",
                stylePlanId
        );

        for (StylePlanPlaceLink link : links) {
            jdbcTemplate.update(
                    """
                    INSERT INTO style_plan_places (
                        style_plan_id,
                        place_id,
                        rank_order,
                        reason
                    )
                    VALUES (?, ?, ?, ?)
                    """,
                    stylePlanId,
                    link.placeId(),
                    link.rank(),
                    link.reasonCode()
            );
        }
    }

    public record StylePlanPlaceLink(
            long placeId,
            int rank,
            String reasonCode
    ) {
    }
}
