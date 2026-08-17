package org.likelionhsu.hackathon.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.likelionhsu.hackathon.place.client.ExternalPlace;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.springframework.beans.factory.annotation.Autowired;
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
        "app.place.kakao.rest-api-key="
})
class PlaceRepositoryIntegrationTest {

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
    PlaceRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private long userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM style_plan_places");
        jdbcTemplate.update("DELETE FROM saved_places");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");

        userId = insertUser();
    }

    @Test
    void upsertReusesProviderIdentityAndUpdatesSnapshot() {
        var first = repository.upsert(place(
                "100",
                "성수 카페",
                "서울 성동구 구주소"
        ));
        var second = repository.upsert(place(
                "100",
                "성수 카페 리뉴얼",
                "서울 성동구 새주소"
        ));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.name()).isEqualTo("성수 카페 리뉴얼");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM places WHERE provider='KAKAO' AND provider_place_id='100'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void savedLookupIsScopedByUser() {
        var stored = repository.upsert(place("200", "저장 카페", "서울 성동구"));
        jdbcTemplate.update(
                "INSERT INTO saved_places (user_id, place_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))",
                userId,
                stored.id()
        );

        assertThat(repository.findSavedPlaceIds(
                userId,
                List.of(stored.id())
        )).containsExactly(stored.id());
    }

    private ExternalPlace place(
            String providerPlaceId,
            String name,
            String address
    ) {
        return new ExternalPlace(
                providerPlaceId,
                name,
                PlaceCategory.CAFE,
                "음식점 > 카페",
                address,
                "서울 성동구 성수이로",
                new BigDecimal("37.5412000"),
                new BigDecimal("127.0563000"),
                "https://place.map.kakao.com/" + providerPlaceId
        );
    }

    private long insertUser() {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    email, nickname, gender, role, status,
                    notification_email_verified, version,
                    created_at, updated_at
                )
                VALUES (
                    'place-test@example.com', '장소테스트',
                    'NOT_SPECIFIED', 'USER', 'ACTIVE',
                    FALSE, 0,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email='place-test@example.com'",
                Long.class
        );
    }
}
