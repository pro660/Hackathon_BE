package org.likelionhsu.hackathon.purchaseutility.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
class PurchaseUtilityAiJobGatewayIntegrationTest {

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
    PurchaseUtilityAiJobGateway gateway;

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
                        "purchase-ai-job@example.com",
                        "구매활용성",
                        Gender.NOT_SPECIFIED
                )
        );

        userId = user.getId();
    }

    @Test
    void pendingJobCanBeClaimedAndSucceeded() {
        long jobId = gateway.createPending(
                userId,
                "purchase-utility-key-1",
                "gpt-model",
                "purchase-utility-v1",
                "a".repeat(64)
        );

        PurchaseUtilityAiJobData pending =
                gateway
                        .findByUserAndIdempotencyKey(
                                userId,
                                "purchase-utility-key-1"
                        )
                        .orElseThrow();

        assertThat(pending.id()).isEqualTo(jobId);
        assertThat(pending.status())
                .isEqualTo(
                        PurchaseUtilityAiJobStatus.PENDING
                );
        assertThat(pending.retryCount()).isZero();

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

        assertThat(
                gateway.markSucceeded(
                        userId,
                        jobId,
                        """
                        {
                          "status":"READY",
                          "analysisId":"801"
                        }
                        """,
                        120,
                        45,
                        850L
                )
        ).isTrue();

        PurchaseUtilityAiJobData succeeded =
                gateway
                        .findOwned(
                                userId,
                                jobId
                        )
                        .orElseThrow();

        assertThat(succeeded.status())
                .isEqualTo(
                        PurchaseUtilityAiJobStatus.SUCCEEDED
                );
        assertThat(succeeded.resultJson())
                .contains("\"READY\"");
        assertThat(succeeded.fallbackJson()).isNull();
        assertThat(succeeded.startedAt()).isNotNull();
        assertThat(succeeded.completedAt()).isNotNull();

        assertThat(
                gateway.findRecentSucceededByInputHash(
                        userId,
                        "a".repeat(64),
                        "purchase-utility-v1",
                        "gpt-model",
                        Instant.now().minusSeconds(172800)
                )
        ).isPresent();
    }

    @Test
    void processingJobCanFailWithRuleBasedFallback() {
        long jobId = gateway.createPending(
                userId,
                "purchase-utility-key-2",
                "gpt-model",
                "purchase-utility-v1",
                "b".repeat(64)
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
                        """
                        {
                          "type":"RULE_BASED",
                          "result":{
                            "status":"READY",
                            "analysisId":"802",
                            "utilityScore":77.0
                          }
                        }
                        """,
                        "OPENAI_UNAVAILABLE",
                        1,
                        1200L
                )
        ).isTrue();

        PurchaseUtilityAiJobData failed =
                gateway
                        .findOwned(
                                userId,
                                jobId
                        )
                        .orElseThrow();

        assertThat(failed.status())
                .isEqualTo(
                        PurchaseUtilityAiJobStatus.FAILED
                );
        assertThat(failed.fallbackJson())
                .contains("\"RULE_BASED\"");
        assertThat(failed.errorCode())
                .isEqualTo("OPENAI_UNAVAILABLE");
        assertThat(failed.retryCount()).isEqualTo(1);
        assertThat(failed.completedAt()).isNotNull();
    }
}
