package org.likelionhsu.hackathon.aijob.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
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
class AiJobJdbcRepositoryIntegrationTest {

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
    AiJobJdbcRepository repository;

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
                        "common-ai-job@example.com",
                        "공통AI작업",
                        Gender.NOT_SPECIFIED
                )
        );

        userId = user.getId();
    }

    @Test
    void stalePendingAndProcessingJobsCanBeTimedOutSafely() {
        long pendingId = repository.createPending(
                userId,
                AiJobType.PURCHASE_UTILITY,
                "stale-pending-key",
                "d".repeat(64),
                "configured-model",
                "purchase-utility-summary-v1"
        );

        long processingId = repository.createPending(
                userId,
                AiJobType.PURCHASE_UTILITY,
                "stale-processing-key",
                "e".repeat(64),
                "configured-model",
                "purchase-utility-summary-v1"
        );

        long freshId = repository.createPending(
                userId,
                AiJobType.PURCHASE_UTILITY,
                "fresh-pending-key",
                "f".repeat(64),
                "configured-model",
                "purchase-utility-summary-v1"
        );

        jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET created_at =
                        DATE_SUB(
                            CURRENT_TIMESTAMP(6),
                            INTERVAL 3 MINUTE
                        ),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """,
                pendingId
        );

        jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'PROCESSING',
                    started_at =
                        DATE_SUB(
                            CURRENT_TIMESTAMP(6),
                            INTERVAL 3 MINUTE
                        ),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """,
                processingId
        );

        assertThat(
                repository.markTimedOutIfStale(
                        userId,
                        pendingId
                )
        ).isTrue();

        assertThat(
                repository.markTimedOutIfStale(
                        userId,
                        processingId
                )
        ).isTrue();

        assertThat(
                repository.markTimedOutIfStale(
                        userId,
                        freshId
                )
        ).isFalse();

        AiJobData pending = repository
                .findOwned(
                        userId,
                        pendingId
                )
                .orElseThrow();

        AiJobData processing = repository
                .findOwned(
                        userId,
                        processingId
                )
                .orElseThrow();

        AiJobData fresh = repository
                .findOwned(
                        userId,
                        freshId
                )
                .orElseThrow();

        assertThat(pending.status())
                .isEqualTo(AiJobStatus.FAILED);
        assertThat(pending.errorCode())
                .isEqualTo("AI_JOB_TIMEOUT");
        assertThat(pending.completedAt())
                .isNotNull();

        assertThat(processing.status())
                .isEqualTo(AiJobStatus.FAILED);
        assertThat(processing.errorCode())
                .isEqualTo("AI_JOB_TIMEOUT");
        assertThat(processing.completedAt())
                .isNotNull();

        assertThat(fresh.status())
                .isEqualTo(AiJobStatus.PENDING);

        assertThat(
                repository.markTimedOutIfStale(
                        userId,
                        pendingId
                )
        ).isFalse();
    }

    @Test
    void pendingJobStoresRequestHashAndStartsWithoutInputHash() {
        String requestHash = "c".repeat(64);

        long jobId = repository.createPending(
                userId,
                AiJobType.PURCHASE_UTILITY,
                "common-ai-job-key-1",
                requestHash,
                "configured-model",
                "purchase-utility-summary-v1"
        );

        AiJobData job = repository
                .findByUserAndIdempotencyKey(
                        userId,
                        "common-ai-job-key-1"
                )
                .orElseThrow();

        assertThat(job.id()).isEqualTo(jobId);
        assertThat(job.userId()).isEqualTo(userId);
        assertThat(job.type()).isEqualTo(AiJobType.PURCHASE_UTILITY);
        assertThat(job.status()).isEqualTo(AiJobStatus.PENDING);
        assertThat(job.requestHash()).isEqualTo(requestHash);
        assertThat(job.inputHash()).isNull();
        assertThat(job.retryCount()).isZero();
        assertThat(job.resultJson()).isNull();
        assertThat(job.fallbackJson()).isNull();
        assertThat(job.createdAt()).isNotNull();
        assertThat(job.updatedAt()).isNotNull();

        assertThat(
                repository.findOwned(
                        userId,
                        jobId
                )
        ).isPresent();

        assertThat(
                repository.findOwned(
                        userId + 1,
                        jobId
                )
        ).isEmpty();
    }
}
