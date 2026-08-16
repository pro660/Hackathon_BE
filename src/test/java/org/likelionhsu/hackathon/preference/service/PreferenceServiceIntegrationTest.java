package org.likelionhsu.hackathon.preference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.preference.dto.request.PreferenceRequest;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationProduct;
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
class PreferenceServiceIntegrationTest {

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
    private PreferenceService preferenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "DELETE FROM preference_profiles"
        );

        jdbcTemplate.update(
                "DELETE FROM ai_jobs"
        );

        jdbcTemplate.update(
                "DELETE FROM users"
        );
    }

    @Test
    void identicalPutPreservesVersionAndUpdatedAt() {
        User user =
                saveUser(
                        "preference-identical@example.com"
                );

        PreferenceRequest request =
                request();

        preferenceService.updatePreference(
                user.getId(),
                request
        );

        Long firstVersion =
                currentVersion(
                        user.getId()
                );

        Timestamp firstUpdatedAt =
                currentUpdatedAt(
                        user.getId()
                );

        preferenceService.updatePreference(
                user.getId(),
                request
        );

        Long secondVersion =
                currentVersion(
                        user.getId()
                );

        Timestamp secondUpdatedAt =
                currentUpdatedAt(
                        user.getId()
                );

        assertThat(secondVersion)
                .isEqualTo(firstVersion);

        assertThat(secondUpdatedAt)
                .isEqualTo(firstUpdatedAt);
    }

    @Test
    void manualPutResetsPersistedAiState() {
        User user =
                saveUser(
                        "preference-ai-reset@example.com"
                );

        PreferenceRequest request =
                request();

        preferenceService.updatePreference(
                user.getId(),
                request
        );

        Long aiJobId =
                insertAiJob(
                        user.getId()
                );

        jdbcTemplate.update(
                """
                UPDATE preference_profiles
                SET summary = ?,
                    confidence = ?,
                    analysis_version = ?,
                    ai_job_id = ?,
                    analyzed_at = CURRENT_TIMESTAMP(6)
                WHERE user_id = ?
                """,
                "AI generated summary",
                0.9000,
                "preference-ai-v1",
                aiJobId,
                user.getId()
        );

        Long versionBeforeReset =
                currentVersion(
                        user.getId()
                );

        preferenceService.updatePreference(
                user.getId(),
                request
        );

        PreferenceRow row =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            summary,
                            confidence,
                            analysis_version,
                            ai_job_id,
                            analyzed_at,
                            version
                        FROM preference_profiles
                        WHERE user_id = ?
                        """,
                        (resultSet, rowNum) ->
                                new PreferenceRow(
                                        resultSet.getString(
                                                "summary"
                                        ),
                                        resultSet.getBigDecimal(
                                                "confidence"
                                        ),
                                        resultSet.getString(
                                                "analysis_version"
                                        ),
                                        resultSet.getObject(
                                                "ai_job_id",
                                                Long.class
                                        ),
                                        resultSet.getTimestamp(
                                                "analyzed_at"
                                        ),
                                        resultSet.getLong(
                                                "version"
                                        )
                                ),
                        user.getId()
                );

        assertThat(row.summary())
                .isNull();

        assertThat(row.confidence())
                .isNull();

        assertThat(row.analysisVersion())
                .isEqualTo(
                        "preference-manual-v1"
                );

        assertThat(row.aiJobId())
                .isNull();

        assertThat(row.analyzedAt())
                .isNull();

        assertThat(row.version())
                .isEqualTo(
                        versionBeforeReset + 1
                );
    }

    private PreferenceRequest request() {
        return new PreferenceRequest(
                List.of("BLACK"),
                List.of("BAG"),
                List.of("CASUAL")
        );
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

    private Long insertAiJob(
            Long userId
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO ai_jobs (
                    user_id,
                    type,
                    status,
                    idempotency_key,
                    model,
                    prompt_version,
                    input_hash,
                    retry_count,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    'PREFERENCE_ANALYSIS',
                    'SUCCEEDED',
                    ?,
                    'test-model',
                    'test-prompt-v1',
                    ?,
                    0,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                userId,
                "preference-ai-reset-" + userId,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        );

        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM ai_jobs
                WHERE user_id = ?
                  AND idempotency_key = ?
                """,
                Long.class,
                userId,
                "preference-ai-reset-" + userId
        );
    }

    private Long currentVersion(
            Long userId
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM preference_profiles
                WHERE user_id = ?
                """,
                Long.class,
                userId
        );
    }

    private Timestamp currentUpdatedAt(
            Long userId
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT updated_at
                FROM preference_profiles
                WHERE user_id = ?
                """,
                Timestamp.class,
                userId
        );
    }

    private record PreferenceRow(
            String summary,
            java.math.BigDecimal confidence,
            String analysisVersion,
            Long aiJobId,
            Timestamp analyzedAt,
            long version
    ) {
    }
}