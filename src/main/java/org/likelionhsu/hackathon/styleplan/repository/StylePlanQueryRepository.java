package org.likelionhsu.hackathon.styleplan.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.styleplan.domain.StyleItemRole;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanWeatherCondition;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanDetailResponse;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StylePlanQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public StylePlanQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<StylePlanListItemResponse> findPage(
            Long userId,
            StylePlanStatus status,
            Pageable pageable
    ) {
        String statusCondition = status == null
                ? ""
                : " AND sp.status = ?";

        String sql = """
                SELECT
                    sp.id,
                    sp.title,
                    sp.occasion,
                    sp.planned_at,
                    sp.status,
                    sp.created_at,
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
                    ) AS thumbnail_image_url,
                    (
                        SELECT COUNT(*)
                        FROM style_plan_items spi_count
                        WHERE spi_count.style_plan_id = sp.id
                    ) AS owned_item_count,
                    (
                        SELECT COUNT(*)
                        FROM style_plan_products spp_count
                        WHERE spp_count.style_plan_id = sp.id
                    ) AS recommended_product_count
                FROM style_plans sp
                WHERE sp.user_id = ?
                """
                + statusCondition
                + buildOrderBy(pageable.getSort())
                + " LIMIT ? OFFSET ?";

        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (status != null) {
            params.add(status.name());
        }
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());

        List<StylePlanListItemResponse> content = jdbcTemplate.query(
                sql,
                this::mapListItem,
                params.toArray()
        );

        String countSql = """
                SELECT COUNT(*)
                FROM style_plans sp
                WHERE sp.user_id = ?
                """ + statusCondition;

        List<Object> countParams = new ArrayList<>();
        countParams.add(userId);
        if (status != null) {
            countParams.add(status.name());
        }

        Long total = jdbcTemplate.queryForObject(
                countSql,
                Long.class,
                countParams.toArray()
        );

        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0L : total
        );
    }

    public Optional<Header> findHeader(
            Long userId,
            Long stylePlanId
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    title,
                    occasion,
                    planned_at,
                    weather_condition,
                    description,
                    generation_type,
                    status,
                    version,
                    created_at,
                    updated_at
                FROM style_plans
                WHERE id = ?
                  AND user_id = ?
                LIMIT 1
                """,
                this::mapHeader,
                stylePlanId,
                userId
        ).stream().findFirst();
    }

    public List<StylePlanDetailResponse.OwnedItem> findOwnedItems(
            Long userId,
            Long stylePlanId
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    spi.user_item_id,
                    ui.name,
                    (
                        SELECT ia.secure_url
                        FROM image_assets ia
                        WHERE ia.owner_user_id = ?
                          AND ia.user_item_id = spi.user_item_id
                          AND ia.purpose = 'ITEM'
                          AND ia.status = 'ACTIVE'
                          AND ia.deleted_at IS NULL
                        ORDER BY ia.sort_order ASC, ia.id ASC
                        LIMIT 1
                    ) AS image_url,
                    spi.role,
                    spi.sort_order
                FROM style_plan_items spi
                JOIN user_items ui
                  ON ui.id = spi.user_item_id
                WHERE spi.style_plan_id = ?
                ORDER BY spi.sort_order ASC, spi.id ASC
                """,
                (resultSet, rowNumber) -> new StylePlanDetailResponse.OwnedItem(
                        String.valueOf(resultSet.getLong("user_item_id")),
                        resultSet.getString("name"),
                        resultSet.getString("image_url"),
                        StyleItemRole.valueOf(resultSet.getString("role")),
                        resultSet.getInt("sort_order")
                ),
                userId,
                stylePlanId
        );
    }

    public List<StylePlanDetailResponse.RecommendedProduct> findRecommendedProducts(
            Long stylePlanId
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    spp.product_id,
                    p.name,
                    (
                        SELECT pi.url
                        FROM product_images pi
                        WHERE pi.product_id = spp.product_id
                        ORDER BY pi.is_primary DESC,
                                 pi.sort_order ASC,
                                 pi.id ASC
                        LIMIT 1
                    ) AS image_url,
                    spp.rank_order,
                    spp.reason
                FROM style_plan_products spp
                JOIN products p
                  ON p.id = spp.product_id
                WHERE spp.style_plan_id = ?
                ORDER BY spp.rank_order ASC, spp.id ASC
                """,
                (resultSet, rowNumber) -> new StylePlanDetailResponse.RecommendedProduct(
                        String.valueOf(resultSet.getLong("product_id")),
                        resultSet.getString("name"),
                        resultSet.getString("image_url"),
                        resultSet.getInt("rank_order"),
                        resultSet.getString("reason")
                ),
                stylePlanId
        );
    }

    public List<StylePlanDetailResponse.Place> findPlaces(
            Long userId,
            Long stylePlanId
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    p.id AS place_id,
                    p.name,
                    p.category_name,
                    p.road_address,
                    p.latitude,
                    p.longitude,
                    p.place_url,
                    spp.rank_order,
                    spp.reason,
                    CASE WHEN EXISTS (
                        SELECT 1
                        FROM saved_places saved
                        WHERE saved.user_id = ?
                          AND saved.place_id = p.id
                    ) THEN TRUE ELSE FALSE END AS saved
                FROM style_plan_places spp
                JOIN places p
                  ON p.id = spp.place_id
                WHERE spp.style_plan_id = ?
                ORDER BY spp.rank_order ASC, spp.id ASC
                """,
                (resultSet, rowNumber) -> new StylePlanDetailResponse.Place(
                        String.valueOf(resultSet.getLong("place_id")),
                        resultSet.getString("name"),
                        PlaceCategory.fromCategoryName(
                                resultSet.getString("category_name")
                        ),
                        resultSet.getString("category_name"),
                        resultSet.getString("road_address"),
                        resultSet.getBigDecimal("latitude"),
                        resultSet.getBigDecimal("longitude"),
                        resultSet.getString("place_url"),
                        resultSet.getInt("rank_order"),
                        resultSet.getString("reason"),
                        resultSet.getBoolean("saved")
                ),
                userId,
                stylePlanId
        );
    }

    private StylePlanListItemResponse mapListItem(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        return new StylePlanListItemResponse(
                String.valueOf(resultSet.getLong("id")),
                resultSet.getString("title"),
                StylePlanOccasion.valueOf(resultSet.getString("occasion")),
                toInstant(resultSet.getTimestamp("planned_at")),
                StylePlanStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("thumbnail_image_url"),
                resultSet.getInt("owned_item_count"),
                resultSet.getInt("recommended_product_count"),
                toInstant(resultSet.getTimestamp("created_at"))
        );
    }

    private Header mapHeader(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        String weather = resultSet.getString("weather_condition");

        return new Header(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                StylePlanOccasion.valueOf(resultSet.getString("occasion")),
                toInstant(resultSet.getTimestamp("planned_at")),
                weather == null
                        ? null
                        : StylePlanWeatherCondition.valueOf(weather),
                resultSet.getString("description"),
                StylePlanGenerationType.valueOf(
                        resultSet.getString("generation_type")
                ),
                StylePlanStatus.valueOf(resultSet.getString("status")),
                resultSet.getLong("version"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("updated_at"))
        );
    }

    private String buildOrderBy(Sort sort) {
        Sort effectiveSort = sort == null || sort.isUnsorted()
                ? Sort.by(Sort.Order.desc("createdAt"))
                : sort;

        List<String> clauses = new ArrayList<>();

        for (Sort.Order order : effectiveSort) {
            String column = switch (order.getProperty()) {
                case "createdAt" -> "sp.created_at";
                case "plannedAt" -> "sp.planned_at";
                case "title" -> "sp.title";
                default -> throw new IllegalArgumentException(
                        "지원하지 않는 StylePlan 정렬 필드입니다."
                );
            };

            clauses.add(
                    column + (order.isAscending() ? " ASC" : " DESC")
            );
        }

        clauses.add("sp.id DESC");
        return " ORDER BY " + String.join(", ", clauses);
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record Header(
            Long id,
            String title,
            StylePlanOccasion occasion,
            Instant plannedAt,
            StylePlanWeatherCondition weatherCondition,
            String description,
            StylePlanGenerationType generationType,
            StylePlanStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
