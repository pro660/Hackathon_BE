package org.likelionhsu.hackathon.preference.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationProduct;
import org.likelionhsu.hackathon.preference.entity.PreferenceStyleTag;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
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
@SpringBootTest
@EntityScan(basePackageClasses = {
        PreferenceProfile.class,
        User.class,
        Product.class,
        Wishlist.class,
        Recommendation.class,
        RecommendationProduct.class,
        UserItem.class
})
class PreferenceRepositoryIntegrationTest {

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
    private PreferenceRepository preferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "DELETE FROM preference_profiles"
        );

        jdbcTemplate.update(
                "DELETE FROM users"
        );
    }

    @Test
    void preferenceCanBeFoundByUserId() {
        User user =
                saveUser(
                        "preference-repository@example.com"
                );

        PreferenceProfile profile =
                PreferenceProfile.createManual(
                        user,
                        List.of(
                                ColorGroup.BLACK
                        ),
                        List.of(
                                ItemCategory.BAG
                        ),
                        List.of(
                                PreferenceStyleTag.CASUAL
                        )
                );

        preferenceRepository.saveAndFlush(
                profile
        );

        assertThat(
                preferenceRepository
                        .findByUser_Id(
                                user.getId()
                        )
        ).isPresent();
    }

    @Test
    void enumListsAreStoredAndReadAsJson() {
        User user =
                saveUser(
                        "preference-json@example.com"
                );

        PreferenceProfile profile =
                PreferenceProfile.createManual(
                        user,
                        List.of(
                                ColorGroup.BEIGE,
                                ColorGroup.BLACK,
                                ColorGroup.WHITE
                        ),
                        List.of(
                                ItemCategory.BAG,
                                ItemCategory.CLOTHING,
                                ItemCategory.SHOES
                        ),
                        List.of(
                                PreferenceStyleTag.CASUAL,
                                PreferenceStyleTag.NEAT
                        )
                );

        preferenceRepository.saveAndFlush(
                profile
        );

        entityManager.clear();

        PreferenceProfile saved =
                preferenceRepository
                        .findByUser_Id(
                                user.getId()
                        )
                        .orElseThrow();

        assertThat(
                saved.getPreferredColors()
        ).containsExactly(
                ColorGroup.BEIGE,
                ColorGroup.BLACK,
                ColorGroup.WHITE
        );

        assertThat(
                saved.getPreferredCategories()
        ).containsExactly(
                ItemCategory.BAG,
                ItemCategory.CLOTHING,
                ItemCategory.SHOES
        );

        assertThat(
                saved.getPreferredStyleTags()
        ).containsExactly(
                PreferenceStyleTag.CASUAL,
                PreferenceStyleTag.NEAT
        );

        assertThat(
                saved.getAnalysisVersion()
        ).isEqualTo(
                "preference-manual-v1"
        );
    }

    @Test
    void flywayV4AndPreferenceUniqueConstraintAreApplied() {
        Integer v4Count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE version = '4'
                          AND success = 1
                        """,
                        Integer.class
                );

        Integer uniqueConstraintCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.table_constraints
                        WHERE constraint_schema = DATABASE()
                          AND table_name = 'preference_profiles'
                          AND constraint_name =
                              'uk_preference_profiles_user_id'
                          AND constraint_type = 'UNIQUE'
                        """,
                        Integer.class
                );

        assertThat(v4Count)
                .isEqualTo(1);

        assertThat(uniqueConstraintCount)
                .isEqualTo(1);
    }

    private User saveUser(
            String email
    ) {
        return userRepository.saveAndFlush(
                User.local(
                        email,
                        "preference-user",
                        Gender.NOT_SPECIFIED
                )
        );
    }
}