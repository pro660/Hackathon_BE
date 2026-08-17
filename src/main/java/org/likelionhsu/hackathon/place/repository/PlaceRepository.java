package org.likelionhsu.hackathon.place.repository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.place.client.ExternalPlace;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.domain.PlaceProvider;
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
}
