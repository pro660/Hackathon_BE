package org.likelionhsu.hackathon.styleplan.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(properties = "app.product-import.enabled=false")
@EntityScan(basePackageClasses = {
        PurchaseUtilityAnalysis.class,
        Recommendation.class,
        User.class,
        Product.class,
        Wishlist.class,
        PreferenceProfile.class,
        UserItem.class
})
class StylePlanQueryRepositoryIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                mysql::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                mysql::getUsername
        );
        registry.add(
                "spring.datasource.password",
                mysql::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver"
        );
        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    StylePlanQueryRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private long userId;
    private long otherUserId;
    private long productId;
    private long secondProductId;
    private long userItemId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM style_plan_places");
        jdbcTemplate.update("DELETE FROM style_plan_products");
        jdbcTemplate.update("DELETE FROM style_plan_items");
        jdbcTemplate.update("DELETE FROM style_plans");
        jdbcTemplate.update("DELETE FROM image_assets");
        jdbcTemplate.update("DELETE FROM user_items");
        jdbcTemplate.update("DELETE FROM product_images");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");

        userId = insertUser(
                "style-query@example.com",
                "스타일조회"
        );
        otherUserId = insertUser(
                "other-style-query@example.com",
                "다른사용자"
        );

        productId = insertProduct(
                "STYLE-QUERY-001",
                "Aren Shopper"
        );
        secondProductId = insertProduct(
                "STYLE-QUERY-002",
                "Stark Backpack"
        );

        insertProductImage(
                productId,
                "https://example.com/product-primary.webp",
                true,
                0
        );
        insertProductImage(
                secondProductId,
                "https://example.com/product-second.webp",
                true,
                0
        );

        userItemId = insertUserItem(
                userId,
                "브라운 데일리백"
        );

        insertItemImage(
                userId,
                userItemId,
                "https://example.com/item-primary.webp",
                0
        );
    }

    @Test
    void listFiltersPaginatesSortsAndPrefersOwnedItemThumbnail() {
        long olderConfirmed = insertStylePlan(
                userId,
                "B 룩",
                "CONFIRMED",
                "2026-08-20T10:00:00Z",
                "2026-08-18T00:00:00Z",
                0
        );
        long newerConfirmed = insertStylePlan(
                userId,
                "A 룩",
                "CONFIRMED",
                "2026-08-22T10:00:00Z",
                "2026-08-18T02:00:00Z",
                0
        );
        insertStylePlan(
                userId,
                "C 룩",
                "DRAFT",
                "2026-08-21T10:00:00Z",
                "2026-08-18T01:00:00Z",
                0
        );
        insertStylePlan(
                otherUserId,
                "다른 사용자 룩",
                "CONFIRMED",
                "2026-08-30T10:00:00Z",
                "2026-08-18T03:00:00Z",
                0
        );

        insertStylePlanItem(
                newerConfirmed,
                userItemId,
                "BAG",
                0
        );
        insertStylePlanProduct(
                newerConfirmed,
                productId,
                1,
                "추천 이유"
        );
        insertStylePlanProduct(
                olderConfirmed,
                secondProductId,
                1,
                "상품 이미지 fallback"
        );

        var firstPage = repository.findPage(
                userId,
                StylePlanStatus.CONFIRMED,
                PageRequest.of(
                        0,
                        1,
                        Sort.by(
                                Sort.Order.desc("createdAt")
                        )
                )
        );

        assertThat(firstPage.getTotalElements())
                .isEqualTo(2L);
        assertThat(firstPage.getTotalPages())
                .isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.getContent())
                .hasSize(1);
        assertThat(firstPage.getContent().getFirst().stylePlanId())
                .isEqualTo(
                        String.valueOf(newerConfirmed)
                );
        assertThat(
                firstPage.getContent()
                        .getFirst()
                        .thumbnailImageUrl()
        ).isEqualTo(
                "https://example.com/item-primary.webp"
        );
        assertThat(
                firstPage.getContent()
                        .getFirst()
                        .ownedItemCount()
        ).isEqualTo(1);
        assertThat(
                firstPage.getContent()
                        .getFirst()
                        .recommendedProductCount()
        ).isEqualTo(1);

        var secondPage = repository.findPage(
                userId,
                StylePlanStatus.CONFIRMED,
                PageRequest.of(
                        1,
                        1,
                        Sort.by(
                                Sort.Order.desc("createdAt")
                        )
                )
        );

        assertThat(secondPage.getContent())
                .hasSize(1);
        assertThat(
                secondPage.getContent()
                        .getFirst()
                        .stylePlanId()
        ).isEqualTo(
                String.valueOf(olderConfirmed)
        );
        assertThat(
                secondPage.getContent()
                        .getFirst()
                        .thumbnailImageUrl()
        ).isEqualTo(
                "https://example.com/product-second.webp"
        );

        var titleAscending = repository.findPage(
                userId,
                StylePlanStatus.CONFIRMED,
                PageRequest.of(
                        0,
                        20,
                        Sort.by(
                                Sort.Order.asc("title")
                        )
                )
        );

        assertThat(
                titleAscending.getContent()
                        .stream()
                        .map(item -> item.title())
                        .toList()
        ).containsExactly(
                "A 룩",
                "B 룩"
        );

        var plannedAtDescending = repository.findPage(
                userId,
                StylePlanStatus.CONFIRMED,
                PageRequest.of(
                        0,
                        20,
                        Sort.by(
                                Sort.Order.desc("plannedAt")
                        )
                )
        );

        assertThat(
                plannedAtDescending.getContent()
                        .stream()
                        .map(item -> item.stylePlanId())
                        .toList()
        ).containsExactly(
                String.valueOf(newerConfirmed),
                String.valueOf(olderConfirmed)
        );
    }

    @Test
    void detailJoinsStoredCompositionAndScopesHeaderByOwner() {
        long stylePlanId = insertStylePlan(
                userId,
                "데이트 룩",
                "CONFIRMED",
                "2026-08-20T10:00:00Z",
                "2026-08-18T00:00:00Z",
                3
        );

        insertStylePlanItem(
                stylePlanId,
                userItemId,
                "BAG",
                0
        );
        insertStylePlanProduct(
                stylePlanId,
                productId,
                1,
                "전체 색상 톤과 잘 어울려요."
        );

        var header = repository.findHeader(
                userId,
                stylePlanId
        ).orElseThrow();

        assertThat(header.title())
                .isEqualTo("데이트 룩");
        assertThat(header.status())
                .isEqualTo(
                        StylePlanStatus.CONFIRMED
                );
        assertThat(header.version())
                .isEqualTo(3L);

        assertThat(
                repository.findHeader(
                        otherUserId,
                        stylePlanId
                )
        ).isEmpty();

        var ownedItems = repository.findOwnedItems(
                userId,
                stylePlanId
        );

        assertThat(ownedItems)
                .hasSize(1);
        assertThat(ownedItems.getFirst().myItemId())
                .isEqualTo(
                        String.valueOf(userItemId)
                );
        assertThat(ownedItems.getFirst().name())
                .isEqualTo("브라운 데일리백");
        assertThat(ownedItems.getFirst().imageUrl())
                .isEqualTo(
                        "https://example.com/item-primary.webp"
                );
        assertThat(ownedItems.getFirst().sortOrder())
                .isZero();

        var products =
                repository.findRecommendedProducts(
                        stylePlanId
                );

        assertThat(products)
                .hasSize(1);
        assertThat(products.getFirst().productId())
                .isEqualTo(
                        String.valueOf(productId)
                );
        assertThat(products.getFirst().name())
                .isEqualTo("Aren Shopper");
        assertThat(products.getFirst().imageUrl())
                .isEqualTo(
                        "https://example.com/product-primary.webp"
                );
        assertThat(products.getFirst().rank())
                .isEqualTo(1);
        assertThat(products.getFirst().reason())
                .isEqualTo(
                        "전체 색상 톤과 잘 어울려요."
                );
    }

    private long insertUser(
            String email,
            String nickname
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    email,
                    nickname,
                    gender,
                    role,
                    status,
                    notification_email_verified,
                    version,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?, ?, 'NOT_SPECIFIED', 'USER', 'ACTIVE',
                    FALSE, 0,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """,
                email,
                nickname
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }

    private long insertProduct(
            String sku,
            String name
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO products (
                    brand,
                    sku,
                    name,
                    category,
                    price,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (
                    'MCM', ?, ?, 'BAG', 1000000, 'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                sku,
                name
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE sku = ?",
                Long.class,
                sku
        );
    }

    private void insertProductImage(
            long productId,
            String url,
            boolean primary,
            int sortOrder
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO product_images (
                    product_id,
                    url,
                    public_id,
                    alt_text,
                    sort_order,
                    is_primary,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?, ?, NULL, NULL, ?, ?,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                productId,
                url,
                sortOrder,
                primary
        );
    }

    private long insertUserItem(
            long ownerUserId,
            String name
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO user_items (
                    user_id,
                    product_id,
                    brand_name,
                    name,
                    category,
                    version,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?, NULL, 'MCM', ?, 'BAG', 0,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                ownerUserId,
                name
        );

        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM user_items
                WHERE user_id = ?
                  AND name = ?
                """,
                Long.class,
                ownerUserId,
                name
        );
    }

    private void insertItemImage(
            long ownerUserId,
            long targetUserItemId,
            String secureUrl,
            int sortOrder
    ) {
        jdbcTemplate.update(
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
                    ?, 'ITEM', ?, NULL, ?, ?, 'jpg',
                    1024, 100, 100, 'ACTIVE', ?,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    NULL
                )
                """,
                ownerUserId,
                targetUserItemId,
                "style-query-item-" + targetUserItemId,
                secureUrl,
                sortOrder
        );
    }

    private long insertStylePlan(
            long ownerUserId,
            String title,
            String status,
            String plannedAt,
            String createdAt,
            long version
    ) {
        Instant planned = Instant.parse(plannedAt);
        Instant created = Instant.parse(createdAt);

        jdbcTemplate.update(
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
                    ?, ?, 'DATE', ?, NULL, NULL,
                    '통합 테스트 스타일 플랜',
                    'MANUAL', ?, NULL, ?, ?, ?
                )
                """,
                ownerUserId,
                title,
                Timestamp.from(planned),
                status,
                version,
                Timestamp.from(created),
                Timestamp.from(created)
        );

        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM style_plans
                WHERE user_id = ?
                  AND title = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                Long.class,
                ownerUserId,
                title
        );
    }

    private void insertStylePlanItem(
            long stylePlanId,
            long targetUserItemId,
            String role,
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
                targetUserItemId,
                role,
                sortOrder
        );
    }

    private void insertStylePlanProduct(
            long stylePlanId,
            long targetProductId,
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
                targetProductId,
                rank,
                reason
        );
    }
}
