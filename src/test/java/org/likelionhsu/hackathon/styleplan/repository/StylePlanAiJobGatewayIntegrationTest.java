package org.likelionhsu.hackathon.styleplan.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
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
class StylePlanAiJobGatewayIntegrationTest {

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
    StylePlanAiJobGateway gateway;

    @Autowired
    AiJobJdbcRepository aiJobRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM ai_jobs");
        jdbcTemplate.update("DELETE FROM users");

        User user = userRepository.saveAndFlush(
                User.local(
                        "style-cache@example.com",
                        "스타일캐시",
                        Gender.NOT_SPECIFIED
                )
        );

        userId = user.getId();
    }

    @Test
    void reusableStylePlanResultUsesCurrentPreviewId() {
        String inputHash = "c".repeat(64);

        long cachedJobId = createProcessingJob(
                "cached-style-key",
                "a".repeat(64)
        );

        assertThat(
                gateway.updateInputHashIfProcessing(
                        userId,
                        cachedJobId,
                        inputHash
                )
        ).isTrue();

        assertThat(
                gateway.markSucceeded(
                        userId,
                        cachedJobId,
                        """
                        {
                          "previewId":"job:1",
                          "title":"데이트 룩",
                          "description":"캐시 결과",
                          "ownedItems":[],
                          "recommendedProducts":[],
                          "generationType":"AI"
                        }
                        """,
                        120,
                        50,
                        700L,
                        0
                )
        ).isTrue();

        long currentJobId = createProcessingJob(
                "current-style-key",
                "b".repeat(64)
        );

        assertThat(
                gateway.updateInputHashIfProcessing(
                        userId,
                        currentJobId,
                        inputHash
                )
        ).isTrue();

        String reusable = gateway
                .findReusableResultJson(
                        userId,
                        currentJobId,
                        inputHash
                )
                .orElseThrow();

        assertThat(reusable)
                .contains(
                        "\"previewId\": \"job:"
                                + currentJobId
                                + "\""
                )
                .contains("\"title\": \"데이트 룩\"");
    }

    @Test
    void resultOlderThanTwentyFourHoursIsNotReusable() {
        String inputHash = "d".repeat(64);

        long cachedJobId = createProcessingJob(
                "expired-cache-key",
                "a".repeat(64)
        );

        assertThat(
                gateway.updateInputHashIfProcessing(
                        userId,
                        cachedJobId,
                        inputHash
                )
        ).isTrue();

        assertThat(
                gateway.markSucceeded(
                        userId,
                        cachedJobId,
                        """
                        {
                          "previewId":"job:1",
                          "title":"오래된 룩",
                          "description":"expired",
                          "ownedItems":[],
                          "recommendedProducts":[],
                          "generationType":"AI"
                        }
                        """,
                        100,
                        40,
                        500L,
                        0
                )
        ).isTrue();

        jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET completed_at =
                    CURRENT_TIMESTAMP(6) - INTERVAL 25 HOUR
                WHERE id = ?
                """,
                cachedJobId
        );

        long currentJobId = createProcessingJob(
                "expired-current-key",
                "b".repeat(64)
        );

        assertThat(
                gateway.updateInputHashIfProcessing(
                        userId,
                        currentJobId,
                        inputHash
                )
        ).isTrue();

        assertThat(
                gateway.findReusableResultJson(
                        userId,
                        currentJobId,
                        inputHash
                )
        ).isEmpty();
    }

    @Test
    void differentModelOrPromptVersionIsNotReusable() {
        String inputHash = "e".repeat(64);

        long cachedJobId = aiJobRepository.createPending(
                userId,
                AiJobType.STYLE_PLAN,
                "different-model-cache-key",
                "a".repeat(64),
                "model-a",
                "style-plan-v1"
        );

        assertThat(
                gateway.claimProcessing(
                        userId,
                        cachedJobId
                )
        ).isTrue();

        assertThat(
                gateway.updateInputHashIfProcessing(
                        userId,
                        cachedJobId,
                        inputHash
                )
        ).isTrue();

        assertThat(
                gateway.markSucceeded(
                        userId,
                        cachedJobId,
                        """
                        {
                          "previewId":"job:1",
                          "title":"모델 A",
                          "description":"result",
                          "ownedItems":[],
                          "recommendedProducts":[],
                          "generationType":"AI"
                        }
                        """,
                        null,
                        null,
                        null,
                        0
                )
        ).isTrue();

        long currentJobId = aiJobRepository.createPending(
                userId,
                AiJobType.STYLE_PLAN,
                "different-model-current-key",
                "b".repeat(64),
                "model-b",
                "style-plan-v1"
        );

        assertThat(
                gateway.claimProcessing(
                        userId,
                        currentJobId
                )
        ).isTrue();

        assertThat(
                gateway.updateInputHashIfProcessing(
                        userId,
                        currentJobId,
                        inputHash
                )
        ).isTrue();

        assertThat(
                gateway.findReusableResultJson(
                        userId,
                        currentJobId,
                        inputHash
                )
        ).isEmpty();
    }

    private long createProcessingJob(
            String idempotencyKey,
            String requestHash
    ) {
        long jobId = aiJobRepository.createPending(
                userId,
                AiJobType.STYLE_PLAN,
                idempotencyKey,
                requestHash,
                "test-model",
                "style-plan-v1"
        );

        assertThat(
                gateway.claimProcessing(
                        userId,
                        jobId
                )
        ).isTrue();

        return jobId;
    }
}
