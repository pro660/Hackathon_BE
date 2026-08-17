package org.likelionhsu.hackathon.home.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
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
@EntityScan(basePackageClasses = {
        PurchaseUtilityAnalysis.class,
        Recommendation.class,
        User.class,
        Product.class,
        Wishlist.class,
        PreferenceProfile.class,
        UserItem.class
})
@SpringBootTest(properties = {
        "app.product-import.enabled=false",
        "app.care-reminders.scheduler.enabled=false"
})
class HomeQueryRepositoryIntegrationTest {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4")
            .withDatabaseName("hackathon_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    HomeQueryRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    long userId;
    long productId;
    long latestStylePlanId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cart_items");
        jdbcTemplate.update("DELETE FROM style_plan_places");
        jdbcTemplate.update("DELETE FROM style_plan_products");
        jdbcTemplate.update("DELETE FROM style_plan_items");
        jdbcTemplate.update("DELETE FROM style_plans");
        jdbcTemplate.update("DELETE FROM recommendation_products");
        jdbcTemplate.update("DELETE FROM recommendations");
        jdbcTemplate.update("DELETE FROM saved_places");
        jdbcTemplate.update("DELETE FROM preference_profiles");
        jdbcTemplate.update("DELETE FROM image_assets");
        jdbcTemplate.update("DELETE FROM user_items");
        jdbcTemplate.update("DELETE FROM product_tag_mappings");
        jdbcTemplate.update("DELETE FROM product_images");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");

        userId = insertUser();
        insertPreference();
        insertUserItems();
        productId = insertProductAndImage();
        insertRecommendation("2026-08-18 00:00:00.000000", "65.00");
        insertRecommendation("2026-08-18 01:00:00.000000", "88.50");
        insertStylePlan("이전 룩", "2026-08-18 00:00:00.000000", false);
        latestStylePlanId = insertStylePlan(
                "최신 데이트 룩",
                "2026-08-18 02:00:00.000000",
                true
        );
    }

    @Test
    void userSummaryCountsOnlyNonDeletedItemsAndPreference() {
        var user = repository.findUserSummary(userId).orElseThrow();
        assertThat(user.nickname()).isEqualTo("홈사용자");
        assertThat(user.status()).isEqualTo("ACTIVE");
        assertThat(user.preferenceCompleted()).isTrue();
        assertThat(user.myItemCount()).isEqualTo(2L);
    }

    @Test
    void latestStylePlanUsesNewestCreatedPlanAndThumbnail() {
        var stylePlan = repository.findLatestStylePlan(userId).orElseThrow();
        assertThat(stylePlan.stylePlanId()).isEqualTo(latestStylePlanId);
        assertThat(stylePlan.title()).isEqualTo("최신 데이트 룩");
        assertThat(stylePlan.thumbnailImageUrl())
                .isEqualTo("https://example.com/product.webp");
    }

    @Test
    void recommendedProductsComeFromLatestStoredRecommendation() {
        var products = repository.findLatestRecommendedProducts(userId);
        assertThat(products).hasSize(1);
        assertThat(products.getFirst().productId()).isEqualTo(productId);
        assertThat(products.getFirst().matchScore()).isEqualByComparingTo("88.50");
        assertThat(products.getFirst().primaryImageUrl())
                .isEqualTo("https://example.com/product.webp");
    }

    private long insertUser() {
        jdbcTemplate.update("""
                INSERT INTO users (
                    email, nickname, gender, role, status,
                    notification_email_verified, version,
                    created_at, updated_at
                ) VALUES (
                    'home@example.com', '홈사용자', 'NOT_SPECIFIED', 'USER', 'ACTIVE',
                    FALSE, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'home@example.com'",
                Long.class
        );
    }

    private void insertPreference() {
        jdbcTemplate.update("""
                INSERT INTO preference_profiles (
                    user_id, preferred_colors, preferred_categories,
                    preferred_style_tags, summary, confidence,
                    analysis_version, ai_job_id, analyzed_at,
                    version, created_at, updated_at
                ) VALUES (
                    ?, JSON_ARRAY(), JSON_ARRAY(), JSON_ARRAY(),
                    NULL, NULL, 'manual-v1', NULL, NULL,
                    0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, userId);
    }

    private void insertUserItems() {
        for (int i = 1; i <= 2; i++) {
            jdbcTemplate.update("""
                    INSERT INTO user_items (
                        user_id, product_id, brand_name, name,
                        category, primary_color, material,
                        material_source, purchase_date, purchase_price,
                        memo, ai_job_id, deleted_at, version,
                        next_care_date, created_at, updated_at
                    ) VALUES (
                        ?, NULL, 'MCM', ?, 'BAG', 'BROWN', 'LEATHER',
                        'USER_CONFIRMED', NULL, NULL, NULL, NULL,
                        NULL, 0, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                    )
                    """, userId, "가방 " + i);
        }
        jdbcTemplate.update("""
                INSERT INTO user_items (
                    user_id, product_id, brand_name, name,
                    category, primary_color, material,
                    material_source, purchase_date, purchase_price,
                    memo, ai_job_id, deleted_at, version,
                    next_care_date, created_at, updated_at
                ) VALUES (
                    ?, NULL, 'MCM', '삭제 가방', 'BAG', 'BLACK', 'LEATHER',
                    'USER_CONFIRMED', NULL, NULL, NULL, NULL,
                    CURRENT_TIMESTAMP(6), 0, NULL,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, userId);
    }

    private long insertProductAndImage() {
        jdbcTemplate.update("""
                INSERT INTO products (
                    brand, sku, name, category, description, price,
                    primary_color, material, product_url, status,
                    created_at, updated_at
                ) VALUES (
                    'MCM', 'HOME-001', 'Aren Shopper', 'BAG', NULL, 1450000,
                    'BROWN', 'LEATHER', NULL, 'ACTIVE',
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """);
        long id = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE sku = 'HOME-001'",
                Long.class
        );
        jdbcTemplate.update("""
                INSERT INTO product_images (
                    product_id, url, public_id, alt_text,
                    sort_order, is_primary, created_at, updated_at
                ) VALUES (
                    ?, 'https://example.com/product.webp', NULL, '상품 이미지',
                    0, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, id);
        return id;
    }

    private void insertRecommendation(String generatedAt, String score) {
        jdbcTemplate.update("""
                INSERT INTO recommendations (
                    user_id, generation_type, summary, context_json,
                    ai_job_id, generated_at, created_at, updated_at
                ) VALUES (
                    ?, 'RULE_BASED', '추천', JSON_OBJECT(), NULL, ?, ?, ?
                )
                """, userId, generatedAt, generatedAt, generatedAt);
        long recommendationId = jdbcTemplate.queryForObject("""
                SELECT id FROM recommendations
                WHERE user_id = ?
                ORDER BY generated_at DESC, id DESC
                LIMIT 1
                """, Long.class, userId);
        jdbcTemplate.update("""
                INSERT INTO recommendation_products (
                    recommendation_id, product_id, rank_order,
                    score, reason, product_snapshot
                ) VALUES (?, ?, 1, ?, '추천 이유', JSON_OBJECT())
                """, recommendationId, productId, new java.math.BigDecimal(score));
    }

    private long insertStylePlan(String title, String createdAt, boolean attachProduct) {
        jdbcTemplate.update("""
                INSERT INTO style_plans (
                    user_id, title, occasion, planned_at,
                    weather_summary, weather_condition, description,
                    generation_type, status, ai_job_id, version,
                    created_at, updated_at
                ) VALUES (
                    ?, ?, 'DATE', NULL, NULL, NULL, NULL,
                    'MANUAL', 'CONFIRMED', NULL, 0, ?, ?
                )
                """, userId, title, createdAt, createdAt);
        long id = jdbcTemplate.queryForObject("""
                SELECT id FROM style_plans
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """, Long.class, userId);
        if (attachProduct) {
            jdbcTemplate.update("""
                    INSERT INTO style_plan_products (
                        style_plan_id, product_id, rank_order, reason
                    ) VALUES (?, ?, 1, '홈 썸네일')
                    """, id, productId);
        }
        return id;
    }
}
