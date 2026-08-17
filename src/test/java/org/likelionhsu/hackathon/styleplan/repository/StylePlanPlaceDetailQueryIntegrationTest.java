package org.likelionhsu.hackathon.styleplan.repository;

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
class StylePlanPlaceDetailQueryIntegrationTest {

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
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver"
        );
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    StylePlanQueryRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    long userId;
    long otherUserId;
    long stylePlanId;
    long placeId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM style_plan_places");
        jdbcTemplate.update("DELETE FROM saved_places");
        jdbcTemplate.update("DELETE FROM style_plan_products");
        jdbcTemplate.update("DELETE FROM style_plan_items");
        jdbcTemplate.update("DELETE FROM style_plans");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");

        userId = insertUser(
                "style-place-detail@example.com",
                "플랜장소"
        );
        otherUserId = insertUser(
                "style-place-other@example.com",
                "다른사용자"
        );

        jdbcTemplate.update(
                """
                INSERT INTO style_plans (
                    user_id, title, occasion, planned_at,
                    weather_summary, weather_condition,
                    description, generation_type, status,
                    ai_job_id, version, created_at, updated_at
                )
                VALUES (
                    ?, '데이트 룩', 'DATE', NULL,
                    NULL, NULL, '설명', 'MANUAL', 'CONFIRMED',
                    NULL, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """,
                userId
        );

        stylePlanId = jdbcTemplate.queryForObject(
                "SELECT id FROM style_plans WHERE user_id = ?",
                Long.class,
                userId
        );

        jdbcTemplate.update(
                """
                INSERT INTO places (
                    provider, provider_place_id, name,
                    category_name, address, road_address,
                    latitude, longitude, place_url,
                    created_at, updated_at
                )
                VALUES (
                    'KAKAO', 'detail-kakao-1', '성수 카페',
                    '음식점 > 카페', '서울', '서울 도로명',
                    37.5412000, 127.0563000,
                    'https://place.map.kakao.com/detail',
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """
        );

        placeId = jdbcTemplate.queryForObject(
                """
                SELECT id FROM places
                WHERE provider_place_id = 'detail-kakao-1'
                """,
                Long.class
        );

        jdbcTemplate.update(
                """
                INSERT INTO style_plan_places (
                    style_plan_id, place_id, rank_order, reason
                )
                VALUES (?, ?, 1, 'OCCASION_CATEGORY_AND_DISTANCE_MATCH')
                """,
                stylePlanId,
                placeId
        );

        jdbcTemplate.update(
                """
                INSERT INTO saved_places (
                    user_id, place_id, created_at
                )
                VALUES (?, ?, CURRENT_TIMESTAMP(6))
                """,
                userId,
                placeId
        );
    }

    @Test
    void findPlacesReturnsRankReasonAndCurrentUsersSavedState() {
        var places = repository.findPlaces(
                userId,
                stylePlanId
        );

        assertThat(places).hasSize(1);
        assertThat(places.getFirst().placeId())
                .isEqualTo(String.valueOf(placeId));
        assertThat(places.getFirst().rank()).isEqualTo(1);
        assertThat(places.getFirst().reasonCode())
                .isEqualTo("OCCASION_CATEGORY_AND_DISTANCE_MATCH");
        assertThat(places.getFirst().saved()).isTrue();

        assertThat(repository.findPlaces(
                otherUserId,
                stylePlanId
        ).getFirst().saved()).isFalse();
    }

    private long insertUser(
            String email,
            String nickname
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    email, nickname, gender, role, status,
                    notification_email_verified, version,
                    created_at, updated_at
                )
                VALUES (
                    ?, ?, 'NOT_SPECIFIED', 'USER', 'ACTIVE',
                    FALSE, 0,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
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
}
