package org.likelionhsu.hackathon.purchaseutility.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
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
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityExplanationGenerationType;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityCompatibleItemSnapshot;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;
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

import jakarta.persistence.EntityManager;

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
class PurchaseUtilityAnalysisRepositoryIntegrationTest {

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
    PurchaseUtilityAnalysisRepository analysisRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "DELETE FROM purchase_utility_analyses"
        );
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void v7AndV12SchemaRoundTripsFactorJson() {
        User user = userRepository.saveAndFlush(
                User.local(
                        "purchase-utility@example.com",
                        "활용성사용자",
                        Gender.NOT_SPECIFIED
                )
        );

        Product product = productRepository.saveAndFlush(
                Product.create(
                        ProductBrand.MCM,
                        "MCM-PU-001",
                        "Aren Shopper",
                        ItemCategory.BAG,
                        null,
                        1_250_000L,
                        ColorGroup.RED,
                        MaterialGroup.LEATHER,
                        null,
                        ProductStatus.ACTIVE
                )
        );

        PurchaseUtilityFactorSnapshot factors =
                new PurchaseUtilityFactorSnapshot(
                        "purchase-utility-rule-v1",
                        PurchaseUtilityExplanationGenerationType.RULE_BASED,
                        new PurchaseUtilityFactorSnapshot.PreferenceFactor(
                                new BigDecimal("20.00"),
                                new BigDecimal("30.00"),
                                true,
                                true,
                                false
                        ),
                        new PurchaseUtilityFactorSnapshot.ItemCombinationFactor(
                                new BigDecimal("18.00"),
                                new BigDecimal("25.00"),
                                2,
                                List.of(
                                        new PurchaseUtilityCompatibleItemSnapshot(
                                                "501",
                                                "베이지 재킷",
                                                ItemCategory.CLOTHING,
                                                ColorGroup.BEIGE,
                                                null,
                                                "구매 후보 제품과 색상 조합이 가능한 아이템입니다."
                                        )
                                )
                        ),
                        new PurchaseUtilityFactorSnapshot.SeasonFactor(
                                new BigDecimal("25.00"),
                                new BigDecimal("25.00"),
                                4,
                                true
                        ),
                        new PurchaseUtilityFactorSnapshot.CategoryCombinationFactor(
                                new BigDecimal("14.00"),
                                new BigDecimal("20.00"),
                                2
                        )
                );

        PurchaseUtilityAnalysis analysis =
                analysisRepository.saveAndFlush(
                        PurchaseUtilityAnalysis.createRuleBased(
                                user,
                                product,
                                new BigDecimal("77.00"),
                                2,
                                factors,
                                "규칙 기반 활용성 분석 결과입니다.",
                                null,
                                Instant.parse(
                                        "2026-08-16T10:30:00Z"
                                )
                        )
                );

        Long analysisId = analysis.getId();
        Long userId = user.getId();
        entityManager.clear();

        PurchaseUtilityAnalysis loaded =
                analysisRepository
                        .findByIdAndUser_Id(
                                analysisId,
                                userId
                        )
                        .orElseThrow();

        assertThat(loaded.getUtilityScore())
                .isEqualByComparingTo("77.00");
        assertThat(loaded.getCompatibleItemCount())
                .isEqualTo(2);
        assertThat(loaded.getFactorJson().ruleVersion())
                .isEqualTo("purchase-utility-rule-v1");
        assertThat(loaded.getFactorJson()
                .itemCombination()
                .compatibleItems())
                .hasSize(1);
        assertThat(loaded.getFactorJson()
                .itemCombination()
                .compatibleItems()
                .getFirst()
                .myItemId())
                .isEqualTo("501");

        Integer v7Count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '7'
                  AND success = 1
                """,
                Integer.class
        );
        Integer v12Count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '12'
                  AND success = 1
                """,
                Integer.class
        );
        Integer removedColumnCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = 'purchase_utility_analyses'
                          AND column_name = 'duplicate_similarity_score'
                        """,
                        Integer.class
                );

        assertThat(v7Count).isEqualTo(1);
        assertThat(v12Count).isEqualTo(1);
        assertThat(removedColumnCount).isZero();
    }
}
