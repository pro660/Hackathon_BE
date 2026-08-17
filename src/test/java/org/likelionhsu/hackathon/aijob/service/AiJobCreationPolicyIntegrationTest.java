package org.likelionhsu.hackathon.aijob.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
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
@SpringBootTest(properties = {
        "app.product-import.enabled=false",
        "OPENAI_DAILY_LIMIT_PER_USER=10"
})
@EntityScan(basePackageClasses = {
        PurchaseUtilityAnalysis.class,
        Recommendation.class,
        User.class,
        Product.class,
        Wishlist.class,
        PreferenceProfile.class,
        UserItem.class
})
class AiJobCreationPolicyIntegrationTest {

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
    AiJobCreationPolicyService policyService;

    @Autowired
    AiJobJdbcRepository aiJobRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private ExecutorService executor;
    private Long userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM ai_jobs");
        jdbcTemplate.update("DELETE FROM users");

        User user = userRepository.saveAndFlush(
                User.local(
                        "a5-policy@example.com",
                        "A5정책",
                        Gender.NOT_SPECIFIED
                )
        );

        userId = user.getId();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdownNow();
        executor.awaitTermination(
                5,
                TimeUnit.SECONDS
        );
    }

    @Test
    void concurrentCreationAllowsExactlyOneRunningJob()
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Attempt> first =
                concurrentAttempt(
                        "concurrent-key-1",
                        ready,
                        start
                );
        Callable<Attempt> second =
                concurrentAttempt(
                        "concurrent-key-2",
                        ready,
                        start
                );

        Future<Attempt> firstFuture =
                executor.submit(first);
        Future<Attempt> secondFuture =
                executor.submit(second);

        assertThat(
                ready.await(
                        5,
                        TimeUnit.SECONDS
                )
        ).isTrue();

        start.countDown();

        List<Attempt> attempts = List.of(
                firstFuture.get(
                        10,
                        TimeUnit.SECONDS
                ),
                secondFuture.get(
                        10,
                        TimeUnit.SECONDS
                )
        );

        assertThat(attempts)
                .filteredOn(Attempt::created)
                .hasSize(1);

        assertThat(attempts)
                .filteredOn(attempt ->
                        attempt.errorCode()
                                == ErrorCode
                                .AI_JOB_ALREADY_RUNNING
                )
                .hasSize(1);

        Integer runningCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ai_jobs
                        WHERE user_id = ?
                          AND status IN (
                              'PENDING',
                              'PROCESSING'
                          )
                        """,
                        Integer.class,
                        userId
                );

        assertThat(runningCount).isEqualTo(1);
    }

    @Test
    void eleventhCreationInsideRollingTwentyFourHoursIsRejected() {
        List<Long> created = new ArrayList<>();

        for (int index = 0; index < 10; index++) {
            long jobId = aiJobRepository.createPending(
                    userId,
                    AiJobType.STYLE_PLAN,
                    "daily-key-" + index,
                    hashFor(index),
                    "test-model",
                    "style-plan-v1"
            );

            created.add(jobId);

            jdbcTemplate.update(
                    """
                    UPDATE ai_jobs
                    SET status = 'SUCCEEDED',
                        result_json = '{}',
                        completed_at = CURRENT_TIMESTAMP(6),
                        updated_at = CURRENT_TIMESTAMP(6)
                    WHERE id = ?
                    """,
                    jobId
            );
        }

        Attempt attempt;

        try {
            policyService.execute(
                    userId,
                    "daily-key-11",
                    () -> aiJobRepository.createPending(
                            userId,
                            AiJobType.STYLE_PLAN,
                            "daily-key-11",
                            "f".repeat(64),
                            "test-model",
                            "style-plan-v1"
                    )
            );

            attempt = Attempt.success();
        } catch (BusinessException exception) {
            attempt = Attempt.failure(
                    exception.getErrorCode()
            );
        }

        assertThat(attempt.created()).isFalse();
        assertThat(attempt.errorCode()).isEqualTo(
                ErrorCode.AI_DAILY_LIMIT_EXCEEDED
        );

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM ai_jobs
                WHERE user_id = ?
                """,
                Integer.class,
                userId
        );

        assertThat(count).isEqualTo(10);
    }

    @Test
    void jobOlderThanTwentyFourHoursDoesNotConsumeLimit() {
        for (int index = 0; index < 10; index++) {
            long jobId = aiJobRepository.createPending(
                    userId,
                    AiJobType.STYLE_PLAN,
                    "old-key-" + index,
                    hashFor(index),
                    "test-model",
                    "style-plan-v1"
            );

            jdbcTemplate.update(
                    """
                    UPDATE ai_jobs
                    SET status = 'SUCCEEDED',
                        result_json = '{}',
                        completed_at =
                            CURRENT_TIMESTAMP(6)
                            - INTERVAL 25 HOUR,
                        created_at =
                            CURRENT_TIMESTAMP(6)
                            - INTERVAL 25 HOUR,
                        updated_at = CURRENT_TIMESTAMP(6)
                    WHERE id = ?
                    """,
                    jobId
            );
        }

        Long createdJobId = policyService.execute(
                userId,
                "new-key-after-window",
                () -> aiJobRepository.createPending(
                        userId,
                        AiJobType.STYLE_PLAN,
                        "new-key-after-window",
                        "e".repeat(64),
                        "test-model",
                        "style-plan-v1"
                )
        );

        assertThat(createdJobId).isPositive();
    }

    private Callable<Attempt> concurrentAttempt(
            String idempotencyKey,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();

            if (!start.await(
                    5,
                    TimeUnit.SECONDS
            )) {
                throw new IllegalStateException(
                        "동시 실행 시작 신호를 받지 못했습니다."
                );
            }

            try {
                policyService.execute(
                        userId,
                        idempotencyKey,
                        () -> aiJobRepository.createPending(
                                userId,
                                AiJobType.STYLE_PLAN,
                                idempotencyKey,
                                idempotencyKey
                                        .equals("concurrent-key-1")
                                        ? "a".repeat(64)
                                        : "b".repeat(64),
                                "test-model",
                                "style-plan-v1"
                        )
                );

                return Attempt.success();
            } catch (BusinessException exception) {
                return Attempt.failure(
                        exception.getErrorCode()
                );
            }
        };
    }

    private String hashFor(int index) {
        char digit = (char) (
                '0' + (index % 10)
        );

        return String.valueOf(digit)
                .repeat(64);
    }

    private record Attempt(
            boolean created,
            ErrorCode errorCode
    ) {

        private static Attempt success() {
            return new Attempt(true, null);
        }

        private static Attempt failure(
                ErrorCode errorCode
        ) {
            return new Attempt(false, errorCode);
        }
    }
}
