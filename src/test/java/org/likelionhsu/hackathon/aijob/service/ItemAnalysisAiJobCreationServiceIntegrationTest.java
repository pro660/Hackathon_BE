package org.likelionhsu.hackathon.aijob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
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
class ItemAnalysisAiJobCreationServiceIntegrationTest {

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
    ItemAnalysisAiJobCreationService service;

    @Autowired
    AiJobJdbcRepository aiJobRepository;

    @Autowired
    ImageAssetJdbcRepository imageAssetRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM image_assets");
        jdbcTemplate.update("DELETE FROM ai_jobs");
        jdbcTemplate.update("DELETE FROM users");

        userId = createUser(
                "item-analysis-owner@example.com",
                "분석이미지소유자"
        );
        otherUserId = createUser(
                "item-analysis-other@example.com",
                "다른사용자"
        );
    }

    @Test
    void pendingJobAndImageBindingCommitTogether() {
        long imageAssetId =
                createTemporaryImage(userId);

        AiJobData created =
                service.createPendingAndBind(
                        userId,
                        imageAssetId,
                        "item-analysis-1",
                        "a".repeat(64),
                        "test-model",
                        "item-analysis-v1"
                );

        assertThat(created.type())
                .isEqualTo(AiJobType.ITEM_ANALYSIS);
        assertThat(created.status())
                .isEqualTo(AiJobStatus.PENDING);

        ImageAssetData image =
                imageAssetRepository
                        .findOwnedItemAsset(
                                userId,
                                imageAssetId
                        )
                        .orElseThrow();

        assertThat(image.aiJobId())
                .isEqualTo(created.id());
    }

    @Test
    void ownershipFailureRollsBackCreatedJob() {
        long imageAssetId =
                createTemporaryImage(userId);

        assertThatThrownBy(() ->
                service.createPendingAndBind(
                        otherUserId,
                        imageAssetId,
                        "item-analysis-other-owner",
                        "b".repeat(64),
                        "test-model",
                        "item-analysis-v1"
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.IMAGE_ASSET_NOT_FOUND
                        )
                );

        assertThat(jobCount(
                otherUserId,
                "item-analysis-other-owner"
        )).isZero();
    }

    @Test
    void runningJobPreventsSecondBindingAndRollsBackNewJob() {
        long imageAssetId =
                createTemporaryImage(userId);

        AiJobData first =
                service.createPendingAndBind(
                        userId,
                        imageAssetId,
                        "item-analysis-running-1",
                        "c".repeat(64),
                        "test-model",
                        "item-analysis-v1"
                );

        assertThatThrownBy(() ->
                service.createPendingAndBind(
                        userId,
                        imageAssetId,
                        "item-analysis-running-2",
                        "d".repeat(64),
                        "test-model",
                        "item-analysis-v1"
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.IMAGE_ASSET_IN_USE
                        )
                );

        assertThat(jobCount(
                userId,
                "item-analysis-running-2"
        )).isZero();

        ImageAssetData image =
                imageAssetRepository
                        .findOwnedItemAsset(
                                userId,
                                imageAssetId
                        )
                        .orElseThrow();

        assertThat(image.aiJobId())
                .isEqualTo(first.id());
    }

    @Test
    void terminalJobAllowsRetryWithSameTemporaryImage() {
        long imageAssetId =
                createTemporaryImage(userId);

        AiJobData first =
                service.createPendingAndBind(
                        userId,
                        imageAssetId,
                        "item-analysis-retry-1",
                        "e".repeat(64),
                        "test-model",
                        "item-analysis-v1"
                );

        jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'FAILED',
                    error_code = 'AI_GENERATION_FAILED',
                    completed_at = CURRENT_TIMESTAMP(6),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """,
                first.id()
        );

        AiJobData retried =
                service.createPendingAndBind(
                        userId,
                        imageAssetId,
                        "item-analysis-retry-2",
                        "f".repeat(64),
                        "test-model",
                        "item-analysis-v1"
                );

        assertThat(retried.id())
                .isNotEqualTo(first.id());

        ImageAssetData image =
                imageAssetRepository
                        .findOwnedItemAsset(
                                userId,
                                imageAssetId
                        )
                        .orElseThrow();

        assertThat(image.aiJobId())
                .isEqualTo(retried.id());
    }

    private Long createUser(
            String email,
            String nickname
    ) {
        return userRepository.saveAndFlush(
                User.local(
                        email,
                        nickname,
                        Gender.NOT_SPECIFIED
                )
        ).getId();
    }

    private long createTemporaryImage(Long ownerUserId) {
        return imageAssetRepository.createTemporaryItem(
                ownerUserId,
                "wear-it/user-items/"
                        + ownerUserId
                        + "-"
                        + System.nanoTime(),
                "https://example.com/item-analysis.jpg",
                "jpg",
                2048L,
                1200,
                900
        );
    }

    private int jobCount(
            Long ownerUserId,
            String idempotencyKey
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM ai_jobs
                WHERE user_id = ?
                  AND idempotency_key = ?
                """,
                Integer.class,
                ownerUserId,
                idempotencyKey
        );

        return count == null ? 0 : count;
    }
}
