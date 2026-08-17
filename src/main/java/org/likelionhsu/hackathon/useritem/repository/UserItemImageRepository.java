package org.likelionhsu.hackathon.useritem.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserItemImageRepository {

    private static final String ACTIVE_ITEM_IMAGE_CONDITION =
            " purpose = 'ITEM'"
                    + " AND status = 'ACTIVE'"
                    + " AND deleted_at IS NULL";

    private final JdbcTemplate jdbcTemplate;

    public UserItemImageRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean lockOwnedActiveItem(
            Long userId,
            Long userItemId
    ) {
        return lockOwnedActiveItemData(
                userId,
                userItemId
        ).isPresent();
    }

    public Optional<LockedUserItemData>
    lockOwnedActiveItemData(
            Long userId,
            Long userItemId
    ) {
        return jdbcTemplate.query(
                """
                SELECT id, ai_job_id
                FROM user_items
                WHERE id = ?
                  AND user_id = ?
                  AND deleted_at IS NULL
                FOR UPDATE NOWAIT
                """,
                (resultSet, rowNumber) -> {
                    long rawAiJobId =
                            resultSet.getLong("ai_job_id");

                    Long aiJobId =
                            resultSet.wasNull()
                                    ? null
                                    : rawAiJobId;

                    return new LockedUserItemData(
                            resultSet.getLong("id"),
                            aiJobId
                    );
                },
                userItemId,
                userId
        ).stream().findFirst();
    }

    public boolean hasItemImageHistory(
            Long userId,
            Long userItemId
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM image_assets
                WHERE owner_user_id = ?
                  AND user_item_id = ?
                  AND purpose = 'ITEM'
                """,
                Integer.class,
                userId,
                userItemId
        );

        return count != null && count > 0;
    }

    public List<UserItemImageData> findActiveImages(
            Long userId,
            Long userItemId
    ) {
        return jdbcTemplate.query(
                "SELECT id, user_item_id, secure_url, sort_order"
                        + " FROM image_assets"
                        + " WHERE owner_user_id = ?"
                        + " AND user_item_id = ?"
                        + " AND"
                        + ACTIVE_ITEM_IMAGE_CONDITION
                        + " ORDER BY sort_order ASC, id ASC",
                (resultSet, rowNumber) ->
                        new UserItemImageData(
                                resultSet.getLong("id"),
                                resultSet.getLong("user_item_id"),
                                resultSet.getString("secure_url"),
                                resultSet.getInt("sort_order")
                        ),
                userId,
                userItemId
        );
    }

    public Map<Long, String> findPrimaryImageUrls(
            Long userId,
            List<Long> userItemIds
    ) {
        if (userItemIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(
                ",",
                Collections.nCopies(
                        userItemIds.size(),
                        "?"
                )
        );

        List<Object> arguments = new ArrayList<>();
        arguments.add(userId);
        arguments.addAll(userItemIds);

        List<UserItemImageData> images = jdbcTemplate.query(
                "SELECT id, user_item_id, secure_url, sort_order"
                        + " FROM image_assets"
                        + " WHERE owner_user_id = ?"
                        + " AND user_item_id IN ("
                        + placeholders
                        + ")"
                        + " AND"
                        + ACTIVE_ITEM_IMAGE_CONDITION
                        + " ORDER BY user_item_id ASC,"
                        + " sort_order ASC, id ASC",
                (resultSet, rowNumber) ->
                        new UserItemImageData(
                                resultSet.getLong("id"),
                                resultSet.getLong("user_item_id"),
                                resultSet.getString("secure_url"),
                                resultSet.getInt("sort_order")
                        ),
                arguments.toArray()
        );

        Map<Long, String> primaryImageUrls =
                new LinkedHashMap<>();

        for (UserItemImageData image : images) {
            primaryImageUrls.putIfAbsent(
                    image.userItemId(),
                    image.url()
            );
        }

        return Map.copyOf(primaryImageUrls);
    }

    public void markDeletePending(
            Long userId,
            Long userItemId
    ) {
        jdbcTemplate.update(
                "UPDATE image_assets"
                        + " SET status = 'DELETE_PENDING'"
                        + " WHERE owner_user_id = ?"
                        + " AND user_item_id = ?"
                        + " AND purpose = 'ITEM'"
                        + " AND status NOT IN"
                        + " ('DELETE_PENDING', 'DELETED')",
                userId,
                userItemId
        );
    }

    public record LockedUserItemData(
            Long id,
            Long aiJobId
    ) {
    }
}
