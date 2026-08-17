package org.likelionhsu.hackathon.home.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HomeQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public HomeQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserSummaryRow> findUserSummary(Long userId) {
        return jdbcTemplate.query(
                """
                SELECT
                    u.nickname,
                    u.status,
                    CASE WHEN EXISTS (
                        SELECT 1
                        FROM preference_profiles pp
                        WHERE pp.user_id = u.id
                    ) THEN TRUE ELSE FALSE END AS preference_completed,
                    (
                        SELECT COUNT(*)
                        FROM user_items ui
                        WHERE ui.user_id = u.id
                          AND ui.deleted_at IS NULL
                    ) AS my_item_count
                FROM users u
                WHERE u.id = ?
                LIMIT 1
                """,
                (rs, rowNum) -> new UserSummaryRow(
                        rs.getString("nickname"),
                        rs.getString("status"),
                        rs.getBoolean("preference_completed"),
                        rs.getLong("my_item_count")
                ),
                userId
        ).stream().findFirst();
    }

    public Optional<LatestStylePlanRow> findLatestStylePlan(Long userId) {
        return jdbcTemplate.query(
                """
                SELECT
                    sp.id,
                    sp.title,
                    COALESCE(
                        (
                            SELECT ia.secure_url
                            FROM style_plan_items spi_thumb
                            JOIN image_assets ia
                              ON ia.user_item_id = spi_thumb.user_item_id
                             AND ia.owner_user_id = sp.user_id
                             AND ia.purpose = 'ITEM'
                             AND ia.status = 'ACTIVE'
                             AND ia.deleted_at IS NULL
                            WHERE spi_thumb.style_plan_id = sp.id
                            ORDER BY spi_thumb.sort_order ASC,
                                     ia.sort_order ASC,
                                     ia.id ASC
                            LIMIT 1
                        ),
                        (
                            SELECT pi.url
                            FROM style_plan_products spp_thumb
                            JOIN product_images pi
                              ON pi.product_id = spp_thumb.product_id
                            WHERE spp_thumb.style_plan_id = sp.id
                            ORDER BY spp_thumb.rank_order ASC,
                                     pi.is_primary DESC,
                                     pi.sort_order ASC,
                                     pi.id ASC
                            LIMIT 1
                        )
                    ) AS thumbnail_image_url
                FROM style_plans sp
                WHERE sp.user_id = ?
                ORDER BY sp.created_at DESC, sp.id DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new LatestStylePlanRow(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("thumbnail_image_url")
                ),
                userId
        ).stream().findFirst();
    }

    public List<RecommendedProductRow> findLatestRecommendedProducts(Long userId) {
        return jdbcTemplate.query(
                """
                SELECT
                    p.id AS product_id,
                    p.name,
                    rp.score,
                    (
                        SELECT pi.url
                        FROM product_images pi
                        WHERE pi.product_id = p.id
                        ORDER BY pi.is_primary DESC,
                                 pi.sort_order ASC,
                                 pi.id ASC
                        LIMIT 1
                    ) AS primary_image_url
                FROM recommendation_products rp
                JOIN products p ON p.id = rp.product_id
                WHERE rp.recommendation_id = (
                    SELECT r.id
                    FROM recommendations r
                    WHERE r.user_id = ?
                    ORDER BY r.generated_at DESC, r.id DESC
                    LIMIT 1
                )
                ORDER BY rp.rank_order ASC, rp.id ASC
                """,
                (rs, rowNum) -> new RecommendedProductRow(
                        rs.getLong("product_id"),
                        rs.getString("name"),
                        rs.getBigDecimal("score"),
                        rs.getString("primary_image_url")
                ),
                userId
        );
    }

    public record UserSummaryRow(
            String nickname,
            String status,
            boolean preferenceCompleted,
            long myItemCount
    ) {}

    public record LatestStylePlanRow(
            long stylePlanId,
            String title,
            String thumbnailImageUrl
    ) {}

    public record RecommendedProductRow(
            long productId,
            String name,
            BigDecimal matchScore,
            String primaryImageUrl
    ) {}
}
