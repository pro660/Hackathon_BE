package org.likelionhsu.hackathon.imageasset.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
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
        "app.image-assets.cleanup.enabled=false"
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
class ImageAssetCleanupRepositoryIntegrationTest {

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
    ImageAssetJdbcRepository repository;

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

        User user = userRepository.saveAndFlush(
                User.local(
                        "cleanup-owner@example.com",
                        "정리대상",
                        Gender.NOT_SPECIFIED
                )
        );
        userId = user.getId();
    }

    @Test
    void expiredTemporaryWithoutRunningAiCanBeQueued() {
        Instant cutoff =
                Instant.parse("2026-08-16T00:00:00Z");

        long imageId = createTemporary("expired");

        jdbcTemplate.update(
                """
                UPDATE image_assets
                SET created_at = ?
                WHERE id = ?
                """,
                Timestamp.from(
                        cutoff.minusSeconds(1)
                ),
                imageId
        );

        List<ImageAssetData> candidates =
                repository
                        .findExpiredTemporaryCandidates(
                                cutoff,
                                100
                        );

        assertThat(candidates)
                .extracting(ImageAssetData::id)
                .containsExactly(imageId);

        assertThat(
                repository
                        .markExpiredTemporaryDeletePending(
                                userId,
                                imageId,
                                cutoff
                        )
        ).isTrue();

        assertThat(
                repository
                        .findOwnedItemAsset(
                                userId,
                                imageId
                        )
                        .orElseThrow()
                        .status()
        ).isEqualTo(
                ImageAssetStatus.DELETE_PENDING
        );
    }

    @Test
    void runningAiProtectsExpiredTemporary() {
        Instant cutoff =
                Instant.parse("2026-08-16T00:00:00Z");

        long imageId = createTemporary("ai-in-use");
        long aiJobId = createAiJob(
                "cleanup-ai-pending",
                "PENDING"
        );

        jdbcTemplate.update(
                """
                UPDATE image_assets
                SET created_at = ?,
                    ai_job_id = ?
                WHERE id = ?
                """,
                Timestamp.from(
                        cutoff.minusSeconds(1)
                ),
                aiJobId,
                imageId
        );

        assertThat(
                repository
                        .findExpiredTemporaryCandidates(
                                cutoff,
                                100
                        )
        ).isEmpty();

        jdbcTemplate.update(
                """
                UPDATE ai_jobs
                SET status = 'SUCCEEDED',
                    completed_at = CURRENT_TIMESTAMP(6),
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """,
                aiJobId
        );

        assertThat(
                repository
                        .findExpiredTemporaryCandidates(
                                cutoff,
                                100
                        )
        )
                .extracting(ImageAssetData::id)
                .containsExactly(imageId);
    }

    @Test
    void pendingRowsAreRestartRecoverable() {
        long imageId = createTemporary("pending-recovery");

        assertThat(
                repository.markDeletePending(
                        userId,
                        imageId
                )
        ).isTrue();

        assertThat(repository.findDeletePending(100))
                .extracting(ImageAssetData::id)
                .contains(imageId);

        assertThat(
                repository.markDeleted(
                        userId,
                        imageId
                )
        ).isTrue();

        ImageAssetData deleted = repository
                .findOwnedItemAsset(userId, imageId)
                .orElseThrow();

        assertThat(deleted.status())
                .isEqualTo(ImageAssetStatus.DELETED);
        assertThat(deleted.deletedAt()).isNotNull();
    }

    private long createTemporary(String suffix) {
        return repository.createTemporaryItem(
                userId,
                "wear-it/user-items/" + suffix,
                "https://example.com/" + suffix + ".jpg",
                "jpg",
                2048L,
                1200,
                900
        );
    }

    private long createAiJob(
            String idempotencyKey,
            String status
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO ai_jobs (
                    user_id,
                    type,
                    status,
                    idempotency_key,
                    request_hash,
                    model,
                    prompt_version,
                    input_hash,
                    retry_count,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    'ITEM_ANALYSIS',
                    ?,
                    ?,
                    NULL,
                    'test-model',
                    'test-v1',
                    NULL,
                    0,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                userId,
                status,
                idempotencyKey
        );

        Long id = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM ai_jobs
                WHERE user_id = ?
                  AND idempotency_key = ?
                """,
                Long.class,
                userId,
                idempotencyKey
        );

        if (id == null) {
            throw new IllegalStateException(
                    "테스트 AI Job 생성에 실패했습니다."
            );
        }

        return id;
    }
}
