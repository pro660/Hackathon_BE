package org.likelionhsu.hackathon.imageasset.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetPurpose;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ImageAssetJdbcRepository {

    private static final String SELECT_SQL = """
            SELECT
                id,
                owner_user_id,
                purpose,
                user_item_id,
                ai_job_id,
                public_id,
                secure_url,
                format,
                bytes,
                width,
                height,
                status,
                sort_order,
                created_at,
                activated_at,
                deleted_at
            FROM image_assets
            """;

    private final JdbcTemplate jdbcTemplate;

    public ImageAssetJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createTemporaryItem(
            Long ownerUserId,
            String publicId,
            String secureUrl,
            String format,
            long bytes,
            int width,
            int height
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        requireText(publicId, "publicId");
        requireText(secureUrl, "secureUrl");
        requireText(format, "format");
        requirePositive(bytes, "bytes");
        requirePositive(width, "width");
        requirePositive(height, "height");

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    """
                                    INSERT INTO image_assets (
                                        owner_user_id,
                                        purpose,
                                        user_item_id,
                                        ai_job_id,
                                        public_id,
                                        secure_url,
                                        format,
                                        bytes,
                                        width,
                                        height,
                                        status,
                                        sort_order,
                                        created_at,
                                        activated_at,
                                        deleted_at
                                    )
                                    VALUES (
                                        ?,
                                        'ITEM',
                                        NULL,
                                        NULL,
                                        ?,
                                        ?,
                                        ?,
                                        ?,
                                        ?,
                                        ?,
                                        'TEMPORARY',
                                        0,
                                        CURRENT_TIMESTAMP(6),
                                        NULL,
                                        NULL
                                    )
                                    """,
                                    Statement.RETURN_GENERATED_KEYS
                            );

                    statement.setLong(1, ownerUserId);
                    statement.setString(2, publicId);
                    statement.setString(3, secureUrl);
                    statement.setString(4, format);
                    statement.setLong(5, bytes);
                    statement.setInt(6, width);
                    statement.setInt(7, height);

                    return statement;
                },
                keyHolder
        );

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException(
                    "생성된 ImageAsset ID를 가져오지 못했습니다."
            );
        }

        return key.longValue();
    }

    public Optional<ImageAssetData> findOwnedItemAsset(
            Long ownerUserId,
            Long imageAssetId
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                imageAssetId,
                "imageAssetId는 null일 수 없습니다."
        );

        return jdbcTemplate.query(
                SELECT_SQL
                        + """
                         WHERE id = ?
                           AND owner_user_id = ?
                           AND purpose = 'ITEM'
                         LIMIT 1
                        """,
                this::map,
                imageAssetId,
                ownerUserId
        ).stream().findFirst();
    }

    public Optional<ImageAssetData> lockOwnedItemAsset(
            Long ownerUserId,
            Long imageAssetId
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                imageAssetId,
                "imageAssetId는 null일 수 없습니다."
        );

        return jdbcTemplate.query(
                SELECT_SQL
                        + """
                         WHERE id = ?
                           AND owner_user_id = ?
                           AND purpose = 'ITEM'
                         FOR UPDATE NOWAIT
                        """,
                this::map,
                imageAssetId,
                ownerUserId
        ).stream().findFirst();
    }

    public java.util.List<ImageAssetData>
    findActiveOwnedItemAssetsForUpdate(
            Long ownerUserId,
            Long userItemId
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                userItemId,
                "userItemId는 null일 수 없습니다."
        );

        return jdbcTemplate.query(
                SELECT_SQL
                        + """
                         WHERE owner_user_id = ?
                           AND user_item_id = ?
                           AND purpose = 'ITEM'
                           AND status = 'ACTIVE'
                           AND deleted_at IS NULL
                         ORDER BY sort_order ASC, id ASC
                         FOR UPDATE
                        """,
                this::map,
                ownerUserId,
                userItemId
        );
    }

    public int markActiveImagesDeletePending(
            Long ownerUserId,
            Long userItemId
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                userItemId,
                "userItemId는 null일 수 없습니다."
        );

        return jdbcTemplate.update(
                """
                UPDATE image_assets
                SET status = 'DELETE_PENDING'
                WHERE owner_user_id = ?
                  AND user_item_id = ?
                  AND purpose = 'ITEM'
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                """,
                ownerUserId,
                userItemId
        );
    }

    public boolean activateTemporaryForItem(
            Long ownerUserId,
            Long imageAssetId,
            Long userItemId
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                imageAssetId,
                "imageAssetId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                userItemId,
                "userItemId는 null일 수 없습니다."
        );

        int updated = jdbcTemplate.update(
                """
                UPDATE image_assets
                SET user_item_id = ?,
                    status = 'ACTIVE',
                    sort_order = 0,
                    activated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND owner_user_id = ?
                  AND purpose = 'ITEM'
                  AND status = 'TEMPORARY'
                  AND user_item_id IS NULL
                  AND deleted_at IS NULL
                """,
                userItemId,
                imageAssetId,
                ownerUserId
        );

        return updated == 1;
    }

    public boolean markLinkedActiveDeletePending(
            Long ownerUserId,
            Long userItemId,
            Long imageAssetId
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                userItemId,
                "userItemId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                imageAssetId,
                "imageAssetId는 null일 수 없습니다."
        );

        int updated = jdbcTemplate.update(
                """
                UPDATE image_assets
                SET status = 'DELETE_PENDING'
                WHERE id = ?
                  AND owner_user_id = ?
                  AND user_item_id = ?
                  AND purpose = 'ITEM'
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                """,
                imageAssetId,
                ownerUserId,
                userItemId
        );

        return updated == 1;
    }

    public boolean markDeletePending(
            Long ownerUserId,
            Long imageAssetId
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                imageAssetId,
                "imageAssetId는 null일 수 없습니다."
        );

        int updated = jdbcTemplate.update(
                """
                UPDATE image_assets
                SET status = 'DELETE_PENDING'
                WHERE id = ?
                  AND owner_user_id = ?
                  AND purpose = 'ITEM'
                  AND status = 'TEMPORARY'
                  AND user_item_id IS NULL
                  AND deleted_at IS NULL
                """,
                imageAssetId,
                ownerUserId
        );

        return updated == 1;
    }

    public boolean markDeleted(
            Long ownerUserId,
            Long imageAssetId
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                imageAssetId,
                "imageAssetId는 null일 수 없습니다."
        );

        int updated = jdbcTemplate.update(
                """
                UPDATE image_assets
                SET status = 'DELETED',
                    deleted_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                  AND owner_user_id = ?
                  AND purpose = 'ITEM'
                  AND status = 'DELETE_PENDING'
                  AND deleted_at IS NULL
                """,
                imageAssetId,
                ownerUserId
        );

        return updated == 1;
    }

    private ImageAssetData map(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        return new ImageAssetData(
                resultSet.getLong("id"),
                resultSet.getLong("owner_user_id"),
                ImageAssetPurpose.valueOf(
                        resultSet.getString("purpose")
                ),
                nullableLong(resultSet, "user_item_id"),
                nullableLong(resultSet, "ai_job_id"),
                resultSet.getString("public_id"),
                resultSet.getString("secure_url"),
                resultSet.getString("format"),
                resultSet.getLong("bytes"),
                resultSet.getInt("width"),
                resultSet.getInt("height"),
                ImageAssetStatus.valueOf(
                        resultSet.getString("status")
                ),
                resultSet.getInt("sort_order"),
                toInstant(resultSet.getTimestamp("created_at")),
                toInstant(resultSet.getTimestamp("activated_at")),
                toInstant(resultSet.getTimestamp("deleted_at"))
        );
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

    private void requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + "는 비어 있을 수 없습니다."
            );
        }
    }

    private void requirePositive(
            long value,
            String field
    ) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    field + "는 0보다 커야 합니다."
            );
        }
    }
}
