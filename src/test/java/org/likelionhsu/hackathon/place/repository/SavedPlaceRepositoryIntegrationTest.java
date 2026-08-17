package org.likelionhsu.hackathon.place.repository;

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
class SavedPlaceRepositoryIntegrationTest {

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
    PlaceRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    long userId;
    long otherUserId;
    long firstPlaceId;
    long secondPlaceId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM style_plan_places");
        jdbcTemplate.update("DELETE FROM saved_places");
        jdbcTemplate.update("DELETE FROM style_plans");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");

        userId = insertUser(
                "saved-place@example.com",
                "저장장소"
        );
        otherUserId = insertUser(
                "saved-place-other@example.com",
                "다른사용자"
        );

        firstPlaceId = insertPlace(
                "saved-kakao-1",
                "첫 장소"
        );
        secondPlaceId = insertPlace(
                "saved-kakao-2",
                "둘째 장소"
        );
    }

    @Test
    void saveIsIdempotentAndScopedByUser() {
        repository.savePlace(userId, firstPlaceId);
        repository.savePlace(userId, firstPlaceId);
        repository.savePlace(otherUserId, firstPlaceId);

        Integer userCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM saved_places
                WHERE user_id = ?
                  AND place_id = ?
                """,
                Integer.class,
                userId,
                firstPlaceId
        );

        assertThat(userCount).isEqualTo(1);
        assertThat(repository.findSavedPlaceIds(
                userId,
                java.util.List.of(firstPlaceId)
        )).containsExactly(firstPlaceId);
        assertThat(repository.findSavedPlaceIds(
                otherUserId,
                java.util.List.of(firstPlaceId)
        )).containsExactly(firstPlaceId);
    }

    @Test
    void savedPageReturnsOnlyCurrentUser() throws Exception {
        repository.savePlace(userId, firstPlaceId);
        Thread.sleep(5L);
        repository.savePlace(userId, secondPlaceId);
        repository.savePlace(otherUserId, firstPlaceId);

        var page = repository.findSavedPage(
                userId,
                PageRequest.of(
                        0,
                        20,
                        Sort.by(Sort.Order.desc("createdAt"))
                )
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(PlaceRepository.SavedPlaceRow::id)
                .containsExactly(secondPlaceId, firstPlaceId);
        assertThat(page.getContent().getFirst().savedAt())
                .isNotNull();
    }

    @Test
    void deleteIsIdempotent() {
        repository.savePlace(userId, firstPlaceId);

        repository.deleteSavedPlace(userId, firstPlaceId);
        repository.deleteSavedPlace(userId, firstPlaceId);

        assertThat(repository.findSavedPlaceIds(
                userId,
                java.util.List.of(firstPlaceId)
        )).isEmpty();
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

    private long insertPlace(
            String providerPlaceId,
            String name
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
                    'KAKAO', ?, ?,
                    '음식점 > 카페', '서울', '서울 도로명',
                    37.5412000, 127.0563000,
                    'https://place.map.kakao.com/test',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                providerPlaceId,
                name
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
