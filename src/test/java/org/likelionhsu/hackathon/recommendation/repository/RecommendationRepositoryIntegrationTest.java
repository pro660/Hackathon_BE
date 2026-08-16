package org.likelionhsu.hackathon.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationFeature;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationOccasion;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationProduct;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationSeason;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationContextSnapshot;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationProductSnapshot;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationProductTagsSnapshot;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationScoreBreakdownSnapshot;

import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import jakarta.persistence.EntityManager;

@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(properties = "app.product-import.enabled=false")
@EntityScan(basePackageClasses = {
        Recommendation.class,
        User.class,
        Product.class,
        Wishlist.class,
        PreferenceProfile.class,
        UserItem.class
})
class RecommendationRepositoryIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired RecommendationRepository recommendationRepository;
    @Autowired RecommendationProductRepository recommendationProductRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM recommendation_products");
        jdbcTemplate.update("DELETE FROM recommendations");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void v7JsonSnapshotsRoundTripWithHibernateValidate() {
        User user = userRepository.saveAndFlush(
                User.local(
                        "recommendation-repository@example.com",
                        "추천사용자",
                        Gender.NOT_SPECIFIED
                )
        );
        Product product = productRepository.saveAndFlush(
                Product.create(
                        ProductBrand.MCM,
                        "MCM-REC-001",
                        "Aren Shopper",
                        ItemCategory.BAG,
                        null,
                        1_250_000L,
                        ColorGroup.BLACK,
                        MaterialGroup.LEATHER,
                        null,
                        ProductStatus.ACTIVE
                )
        );

        RecommendationContextSnapshot context =
                new RecommendationContextSnapshot(
                        "product-recommendation-v1",
                        List.of("CASUAL"),
                        RecommendationOccasion.DATE,
                        RecommendationSeason.AUTUMN,
                        List.of(RecommendationFeature.COMPACT),
                        ItemCategory.BAG,
                        3
                );
        Recommendation recommendation = recommendationRepository.saveAndFlush(
                Recommendation.createRuleBased(
                        user,
                        "추천 요약",
                        context,
                        java.time.Instant.parse("2026-08-16T00:00:00Z")
                )
        );

        RecommendationProductSnapshot snapshot =
                new RecommendationProductSnapshot(
                        String.valueOf(product.getId()),
                        product.getName(),
                        product.getCategory(),
                        product.getPrice(),
                        product.getPrimaryColor(),
                        "https://example.com/product.webp",
                        new RecommendationProductTagsSnapshot(
                                List.of("CASUAL"),
                                List.of("AUTUMN"),
                                List.of("DATE"),
                                List.of("COMPACT")
                        ),
                        new RecommendationScoreBreakdownSnapshot(
                                new BigDecimal("30.00"),
                                new BigDecimal("25.00"),
                                new BigDecimal("25.00"),
                                new BigDecimal("20.00")
                        )
                );
        recommendationProductRepository.saveAndFlush(
                RecommendationProduct.create(
                        recommendation,
                        product,
                        1,
                        new BigDecimal("100.00"),
                        "추천 이유",
                        snapshot
                )
        );

        Long recommendationId = recommendation.getId();
        Long userId = user.getId();
        Long productId = product.getId();
        entityManager.clear();

        Recommendation loaded = recommendationRepository
                .findByIdAndUser_Id(recommendationId, userId)
                .orElseThrow();
        List<RecommendationProduct> products =
                recommendationProductRepository
                        .findAllWithProductByRecommendationId(recommendationId);

        assertThat(loaded.getContextJson().scorePolicyVersion())
                .isEqualTo("product-recommendation-v1");
        assertThat(loaded.getContextJson().preferredStyleTags())
                .containsExactly("CASUAL");
        assertThat(products).hasSize(1);
        assertThat(products.getFirst().getProductSnapshot().productId())
                .isEqualTo(String.valueOf(productId));
        assertThat(products.getFirst().getProductSnapshot()
                .scoreBreakdown().feature())
                .isEqualByComparingTo("20.00");
        assertThat(products.getFirst().getScore())
                .isEqualByComparingTo("100.00");

        Integer v7Count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '7'
                  AND success = 1
                """,
                Integer.class
        );
        assertThat(v7Count).isEqualTo(1);
    }
}
