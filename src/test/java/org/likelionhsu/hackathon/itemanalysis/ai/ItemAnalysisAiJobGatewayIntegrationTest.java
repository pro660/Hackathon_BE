package org.likelionhsu.hackathon.itemanalysis.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
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
class ItemAnalysisAiJobGatewayIntegrationTest {

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
    ItemAnalysisAiJobGateway gateway;

    @Autowired
    AiJobJdbcRepository aiJobRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM image_assets");
        jdbcTemplate.update("DELETE FROM ai_jobs");
        jdbcTemplate.update("DELETE FROM users");

        userId = userRepository.saveAndFlush(
                User.local(
                        "item-analysis-lifecycle@example.com",
                        "아이템분석상태",
                        Gender.NOT_SPECIFIED
                )
        ).getId();
    }

    @Test
    void processingJobCanStoreInputAndSucceed() {
        long jobId = createPending(
                "item-analysis-success"
        );

        assertThat(
                gateway.claimProcessing(
                        userId,
                        jobId
                )
        ).isTrue();

        assertThat(
                gateway.claimProcessing(
                        userId,
                        jobId
                )
        ).isFalse();

        String inputHash = "a".repeat(64);

        assertThat(
                gateway.updateInputHashIfProcessing(
                        userId,
                        jobId,
                        inputHash
                )
        ).isTrue();

        assertThat(
                gateway.markSucceeded(
                        userId,
                        jobId,
                        """
                        {
                          "brandName":"MCM",
                          "name":"백팩",
                          "category":"BAG",
                          "primaryColor":"BLACK",
                          "material":"LEATHER"
                        }
                        """.strip(),
                        120,
                        45,
                        820L,
                        1
                )
        ).isTrue();

        AiJobData job = aiJobRepository
                .findOwned(
                        userId,
                        jobId
                )
                .orElseThrow();

        assertThat(job.status())
                .isEqualTo(AiJobStatus.SUCCEEDED);
        assertThat(job.inputHash())
                .isEqualTo(inputHash);
        assertThat(job.resultJson())
                .contains("\"brandName\": \"MCM\"");
        assertThat(job.inputTokens())
                .isEqualTo(120);
        assertThat(job.outputTokens())
                .isEqualTo(45);
        assertThat(job.latencyMs())
                .isEqualTo(820L);
        assertThat(job.retryCount())
                .isEqualTo(1);
        assertThat(job.errorCode()).isNull();
        assertThat(job.completedAt()).isNotNull();
    }

    @Test
    void processingJobCanFailWithoutFallbackJson() {
        long jobId = createPending(
                "item-analysis-failure"
        );

        assertThat(
                gateway.claimProcessing(
                        userId,
                        jobId
                )
        ).isTrue();

        assertThat(
                gateway.markFailed(
                        userId,
                        jobId,
                        "AI_ITEM_ANALYSIS_FAILED",
                        500L,
                        0
                )
        ).isTrue();

        AiJobData job = aiJobRepository
                .findOwned(
                        userId,
                        jobId
                )
                .orElseThrow();

        assertThat(job.status())
                .isEqualTo(AiJobStatus.FAILED);
        assertThat(job.resultJson()).isNull();
        assertThat(job.fallbackJson()).isNull();
        assertThat(job.errorCode())
                .isEqualTo("AI_ITEM_ANALYSIS_FAILED");
        assertThat(job.latencyMs())
                .isEqualTo(500L);
        assertThat(job.completedAt()).isNotNull();
    }

    private long createPending(
            String idempotencyKey
    ) {
        return aiJobRepository.createPending(
                userId,
                AiJobType.ITEM_ANALYSIS,
                idempotencyKey,
                "b".repeat(64),
                "test-model",
                "item-analysis-v1"
        );
    }
}
