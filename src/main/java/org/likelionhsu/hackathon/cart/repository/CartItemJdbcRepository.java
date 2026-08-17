package org.likelionhsu.hackathon.cart.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.likelionhsu.hackathon.cart.domain.CartItemData;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CartItemJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public CartItemJdbcRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByUser_IdAndProduct_Id(
            Long userId,
            Long productId
    ) {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_items
                        WHERE user_id = ?
                          AND product_id = ?
                        """,
                        Integer.class,
                        userId,
                        productId
                );

        return count != null
                && count > 0;
    }

    public int insertIfAbsent(
            Long userId,
            Long productId
    ) {
        return jdbcTemplate.update(
                """
                INSERT INTO cart_items (
                    user_id,
                    product_id,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                ON DUPLICATE KEY UPDATE
                    id = id
                """,
                userId,
                productId
        );
    }

    public void deleteByUser_IdAndProduct_Id(
            Long userId,
            Long productId
    ) {
        jdbcTemplate.update(
                """
                DELETE FROM cart_items
                WHERE user_id = ?
                  AND product_id = ?
                """,
                userId,
                productId
        );
    }

    public Page<CartItemData> findAllActiveByUserId(
            Long userId,
            Pageable pageable
    ) {
        String direction =
                resolveCreatedAtDirection(pageable);

        String sql =
                """
                SELECT
                    ci.id AS cart_item_id,
                    p.id AS product_id,
                    p.brand,
                    p.name,
                    p.price,
                    (
                        SELECT pi.url
                        FROM product_images pi
                        WHERE pi.product_id = p.id
                          AND pi.is_primary = TRUE
                        ORDER BY
                            pi.sort_order ASC,
                            pi.id ASC
                        LIMIT 1
                    ) AS primary_image_url,
                    p.product_url,
                    ci.created_at
                FROM cart_items ci
                JOIN products p
                  ON p.id = ci.product_id
                WHERE ci.user_id = ?
                  AND p.status = 'ACTIVE'
                ORDER BY
                    ci.created_at %s,
                    ci.id %s
                LIMIT ?
                OFFSET ?
                """.formatted(
                        direction,
                        direction
                );

        List<CartItemData> items =
                jdbcTemplate.query(
                        sql,
                        this::map,
                        userId,
                        pageable.getPageSize(),
                        pageable.getOffset()
                );

        Long total =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_items ci
                        JOIN products p
                          ON p.id = ci.product_id
                        WHERE ci.user_id = ?
                          AND p.status = 'ACTIVE'
                        """,
                        Long.class,
                        userId
                );

        return new PageImpl<>(
                items,
                pageable,
                total == null
                        ? 0L
                        : total
        );
    }

    private String resolveCreatedAtDirection(
            Pageable pageable
    ) {
        Sort.Order order =
                pageable.getSort()
                        .getOrderFor("createdAt");

        if (order == null) {
            return "DESC";
        }

        return order.isAscending()
                ? "ASC"
                : "DESC";
    }

    private CartItemData map(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        return new CartItemData(
                resultSet.getLong("cart_item_id"),
                resultSet.getLong("product_id"),
                ProductBrand.valueOf(
                        resultSet.getString("brand")
                ),
                resultSet.getString("name"),
                resultSet.getLong("price"),
                resultSet.getString(
                        "primary_image_url"
                ),
                resultSet.getString("product_url"),
                toInstant(
                        resultSet.getTimestamp(
                                "created_at"
                        )
                )
        );
    }

    private java.time.Instant toInstant(
            Timestamp timestamp
    ) {
        return timestamp == null
                ? null
                : timestamp.toInstant();
    }
}