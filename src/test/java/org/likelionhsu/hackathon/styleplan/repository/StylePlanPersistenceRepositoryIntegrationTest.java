package org.likelionhsu.hackathon.styleplan.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.likelionhsu.hackathon.styleplan.domain.StyleItemRole;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
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
class StylePlanPersistenceRepositoryIntegrationTest {

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
    StylePlanPersistenceRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private long userId;
    private long otherUserId;
    private long userItemId;
    private long productId;

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
                "style-mutation@example.com",
                "스타일수정"
        );
        otherUserId = insertUser(
                "other-style-mutation@example.com",
                "다른수정"
        );
        productId = insertProduct();
        userItemId = insertUserItem();
    }

    @Test
    void updateUsesOwnerAndVersionAndAllowsPlannedAtClear() {
        long stylePlanId =
                repository.insertPlan(
                        userId,
                        "데이트 룩",
                        StylePlanOccasion.DATE,
                        Instant.parse(
                                "2026-08-20T10:00:00Z"
                        ),
                        null,
                        "설명",
                        StylePlanGenerationType.MANUAL,
                        StylePlanStatus.CONFIRMED,
                        null
                );

        int updated = repository.updateMetadata(
                userId,
                stylePlanId,
                "주말 데이트 룩",
                null,
                StylePlanStatus.COMPLETED,
                0L
        );

        assertThat(updated).isEqualTo(1);

        var row = jdbcTemplate.queryForMap(
                """
                SELECT title, planned_at, status, version
                FROM style_plans
                WHERE id = ?
                """,
                stylePlanId
        );

        assertThat(row.get("title"))
                .isEqualTo("주말 데이트 룩");
        assertThat(row.get("planned_at")).isNull();
        assertThat(row.get("status"))
                .isEqualTo("COMPLETED");
        assertThat(
                ((Number) row.get("version")).longValue()
        ).isEqualTo(1L);

        assertThat(repository.updateMetadata(
                userId,
                stylePlanId,
                "stale",
                null,
                StylePlanStatus.CANCELED,
                0L
        )).isZero();

        assertThat(repository.updateMetadata(
                otherUserId,
                stylePlanId,
                "other",
                null,
                StylePlanStatus.CANCELED,
                1L
        )).isZero();
    }

    @Test
    void deleteHardDeletesLinksButKeepsSourceRows() {
        long stylePlanId =
                repository.insertPlan(
                        userId,
                        "삭제할 룩",
                        StylePlanOccasion.DAILY,
                        null,
                        null,
                        null,
                        StylePlanGenerationType.MANUAL,
                        StylePlanStatus.DRAFT,
                        null
                );

        repository.insertItem(
                stylePlanId,
                userItemId,
                StyleItemRole.BAG,
                0
        );
        repository.insertProduct(
                stylePlanId,
                productId,
                1,
                "추천"
        );

        assertThat(repository.deleteOwnedPlan(
                otherUserId,
                stylePlanId
        )).isZero();

        assertThat(repository.deleteOwnedPlan(
                userId,
                stylePlanId
        )).isEqualTo(1);

        assertThat(count(
                "style_plans",
                stylePlanId
        )).isZero();
        assertThat(countChild(
                "style_plan_items",
                stylePlanId
        )).isZero();
        assertThat(countChild(
                "style_plan_products",
                stylePlanId
        )).isZero();

        assertThat(count(
                "user_items",
                userItemId
        )).isEqualTo(1);
        assertThat(count(
                "products",
                productId
        )).isEqualTo(1);
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

    private long insertProduct() {
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
                    'MCM', 'STYLE-MUTATION-001',
                    'Aren Shopper', 'BAG',
                    1000000, 'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """
        );

        return jdbcTemplate.queryForObject(
                """
                SELECT id FROM products
                WHERE sku = 'STYLE-MUTATION-001'
                """,
                Long.class
        );
    }

    private long insertUserItem() {
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
                    ?, NULL, 'MCM',
                    '브라운 데일리백', 'BAG', 0,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                userId
        );

        return jdbcTemplate.queryForObject(
                """
                SELECT id FROM user_items
                WHERE user_id = ?
                  AND name = '브라운 데일리백'
                """,
                Long.class,
                userId
        );
    }

    private int count(
            String table,
            long id
    ) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM "
                        + table
                        + " WHERE id = ?",
                Integer.class,
                id
        );

        return result == null ? 0 : result;
    }

    private int countChild(
            String table,
            long stylePlanId
    ) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM "
                        + table
                        + " WHERE style_plan_id = ?",
                Integer.class,
                stylePlanId
        );

        return result == null ? 0 : result;
    }
}
