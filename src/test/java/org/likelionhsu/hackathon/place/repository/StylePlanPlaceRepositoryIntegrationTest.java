package org.likelionhsu.hackathon.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
class StylePlanPlaceRepositoryIntegrationTest {

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
    StylePlanPlaceRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    long userId;
    long stylePlanId;
    long firstPlaceId;
    long secondPlaceId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM style_plan_places");
        jdbcTemplate.update("DELETE FROM style_plan_products");
        jdbcTemplate.update("DELETE FROM style_plan_items");
        jdbcTemplate.update("DELETE FROM style_plans");
        jdbcTemplate.update("DELETE FROM saved_places");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update("""
                INSERT INTO users (
                    email, nickname, gender, role, status,
                    notification_email_verified, version,
                    created_at, updated_at
                )
                VALUES (
                    'place-ranking@example.com',
                    '장소추천',
                    'NOT_SPECIFIED',
                    'USER',
                    'ACTIVE',
                    FALSE,
                    0,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """);

        userId = jdbcTemplate.queryForObject(
                """
                SELECT id FROM users
                WHERE email = 'place-ranking@example.com'
                """,
                Long.class
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
                """
                SELECT id FROM style_plans
                WHERE user_id = ?
                """,
                Long.class,
                userId
        );

        firstPlaceId = insertPlace(
                "kakao-1",
                "카페 1",
                "37.5445000",
                "127.0560000"
        );
        secondPlaceId = insertPlace(
                "kakao-2",
                "카페 2",
                "37.5450000",
                "127.0570000"
        );
    }

    @Test
    void replaceDeletesPreviousLinksAndStoresNewRanks() {
        repository.replace(
                stylePlanId,
                List.of(new StylePlanPlaceRepository.StylePlanPlaceLink(
                        firstPlaceId,
                        1,
                        "OCCASION_CATEGORY_AND_DISTANCE_MATCH"
                ))
        );

        repository.replace(
                stylePlanId,
                List.of(new StylePlanPlaceRepository.StylePlanPlaceLink(
                        secondPlaceId,
                        1,
                        "DISTANCE_MATCH"
                ))
        );

        var rows = jdbcTemplate.queryForList(
                """
                SELECT place_id, rank_order, reason
                FROM style_plan_places
                WHERE style_plan_id = ?
                ORDER BY rank_order
                """,
                stylePlanId
        );

        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.getFirst().get("place_id"))
                .longValue()).isEqualTo(secondPlaceId);
        assertThat(((Number) rows.getFirst().get("rank_order"))
                .intValue()).isEqualTo(1);
        assertThat(rows.getFirst().get("reason"))
                .isEqualTo("DISTANCE_MATCH");
    }

    @Test
    void replaceWithEmptyListClearsRecommendations() {
        repository.replace(
                stylePlanId,
                List.of(new StylePlanPlaceRepository.StylePlanPlaceLink(
                        firstPlaceId,
                        1,
                        "DISTANCE_MATCH"
                ))
        );

        repository.replace(stylePlanId, List.of());

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM style_plan_places
                WHERE style_plan_id = ?
                """,
                Integer.class,
                stylePlanId
        );

        assertThat(count).isZero();
    }

    private long insertPlace(
            String providerPlaceId,
            String name,
            String latitude,
            String longitude
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO places (
                    provider, provider_place_id, name,
                    category_name, address, road_address,
                    latitude, longitude, place_url,
                    created_at, updated_at
                )
                VALUES (
                    'KAKAO', ?, ?, '음식점 > 카페',
                    '서울', '서울 도로명',
                    ?, ?,
                    'https://place.map.kakao.com/test',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                providerPlaceId,
                name,
                latitude,
                longitude
        );

        return jdbcTemplate.queryForObject(
                """
                SELECT id FROM places
                WHERE provider = 'KAKAO'
                  AND provider_place_id = ?
                """,
                Long.class,
                providerPlaceId
        );
    }
}
