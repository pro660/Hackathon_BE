package org.likelionhsu.hackathon.place.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.likelionhsu.hackathon.place.client.ExternalPlace;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.domain.PlaceProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlaceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public StoredPlace upsert(ExternalPlace place) {
        jdbcTemplate.update(
                """
                INSERT INTO places (
                    provider,
                    provider_place_id,
                    name,
                    category_name,
                    address,
                    road_address,
                    latitude,
                    longitude,
                    place_url,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?,
                        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    category_name = VALUES(category_name),
                    address = VALUES(address),
                    road_address = VALUES(road_address),
                    latitude = VALUES(latitude),
                    longitude = VALUES(longitude),
                    place_url = VALUES(place_url),
                    updated_at = CURRENT_TIMESTAMP(6)
                """,
                PlaceProvider.KAKAO.name(),
                place.providerPlaceId(),
                place.name(),
                place.categoryName(),
                place.address(),
                place.roadAddress(),
                place.latitude(),
                place.longitude(),
                place.placeUrl()
        );

        return jdbcTemplate.query(
                """
                SELECT id, name, category_name, address, road_address,
                       latitude, longitude, place_url
                FROM places
                WHERE provider = ?
                  AND provider_place_id = ?
                LIMIT 1
                """,
                (resultSet, rowNumber) -> new StoredPlace(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        place.category(),
                        resultSet.getString("category_name"),
                        resultSet.getString("address"),
                        resultSet.getString("road_address"),
                        resultSet.getBigDecimal("latitude"),
                        resultSet.getBigDecimal("longitude"),
                        resultSet.getString("place_url")
                ),
                PlaceProvider.KAKAO.name(),
                place.providerPlaceId()
        ).stream().findFirst().orElseThrow(
                () -> new IllegalStateException("upsert한 Place를 찾을 수 없습니다.")
        );
    }

    public boolean existsById(Long placeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM places WHERE id = ?",
                Integer.class,
                placeId
        );
        return count != null && count > 0;
    }

    public Optional<StoredPlace> findById(Long placeId) {
        return jdbcTemplate.query(
                """
                SELECT id, name, category_name, address, road_address,
                       latitude, longitude, place_url
                FROM places
                WHERE id = ?
                LIMIT 1
                """,
                (resultSet, rowNumber) -> new StoredPlace(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        PlaceCategory.fromCategoryName(
                                resultSet.getString("category_name")
                        ),
                        resultSet.getString("category_name"),
                        resultSet.getString("address"),
                        resultSet.getString("road_address"),
                        resultSet.getBigDecimal("latitude"),
                        resultSet.getBigDecimal("longitude"),
                        resultSet.getString("place_url")
                ),
                placeId
        ).stream().findFirst();
    }

    public void savePlace(Long userId, Long placeId) {
        jdbcTemplate.update(
                """
                INSERT IGNORE INTO saved_places (
                    user_id,
                    place_id,
                    created_at
                )
                VALUES (?, ?, CURRENT_TIMESTAMP(6))
                """,
                userId,
                placeId
        );
    }

    public void deleteSavedPlace(Long userId, Long placeId) {
        jdbcTemplate.update(
                """
                DELETE FROM saved_places
                WHERE user_id = ?
                  AND place_id = ?
                """,
                userId,
                placeId
        );
    }

    public Page<SavedPlaceRow> findSavedPage(
            Long userId,
            Pageable pageable
    ) {
        String sql = """
                SELECT
                    p.id,
                    p.name,
                    p.category_name,
                    p.address,
                    p.road_address,
                    p.latitude,
                    p.longitude,
                    p.place_url,
                    sp.created_at AS saved_at
                FROM saved_places sp
                JOIN places p ON p.id = sp.place_id
                WHERE sp.user_id = ?
                """
                + buildSavedOrderBy(pageable.getSort())
                + " LIMIT ? OFFSET ?";

        List<SavedPlaceRow> content = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new SavedPlaceRow(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        PlaceCategory.fromCategoryName(
                                resultSet.getString("category_name")
                        ),
                        resultSet.getString("category_name"),
                        resultSet.getString("address"),
                        resultSet.getString("road_address"),
                        resultSet.getBigDecimal("latitude"),
                        resultSet.getBigDecimal("longitude"),
                        resultSet.getString("place_url"),
                        toInstant(resultSet.getTimestamp("saved_at"))
                ),
                userId,
                pageable.getPageSize(),
                pageable.getOffset()
        );

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM saved_places
                WHERE user_id = ?
                """,
                Long.class,
                userId
        );

        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0L : total
        );
    }

    public Set<Long> findSavedPlaceIds(
            Long userId,
            List<Long> placeIds
    ) {
        if (placeIds == null || placeIds.isEmpty()) {
            return Set.of();
        }

        String placeholders = String.join(
                ", ",
                placeIds.stream().map(id -> "?").toList()
        );

        Object[] params = new Object[placeIds.size() + 1];
        params[0] = userId;
        for (int index = 0; index < placeIds.size(); index++) {
            params[index + 1] = placeIds.get(index);
        }

        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT place_id FROM saved_places WHERE user_id = ? AND place_id IN ("
                        + placeholders + ")",
                Long.class,
                params
        );
        return new HashSet<>(ids);
    }

    private String buildSavedOrderBy(Sort sort) {
        Sort effectiveSort = sort == null || sort.isUnsorted()
                ? Sort.by(Sort.Order.desc("createdAt"))
                : sort;

        List<String> clauses = new ArrayList<>();

        for (Sort.Order order : effectiveSort) {
            String column = switch (order.getProperty()) {
                case "createdAt" -> "sp.created_at";
                default -> throw new IllegalArgumentException(
                        "지원하지 않는 SavedPlace 정렬 필드입니다."
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

    public record StoredPlace(
            long id,
            String name,
            PlaceCategory category,
            String categoryName,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String placeUrl
    ) {
    }

    public record SavedPlaceRow(
            long id,
            String name,
            PlaceCategory category,
            String categoryName,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String placeUrl,
            Instant savedAt
    ) {
    }
}
