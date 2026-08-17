package org.likelionhsu.hackathon.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
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
class UserDataDeletionImageAssetIntegrationTest {

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
    UserDataDeletionService userDataDeletionService;

    @Autowired
    ImageAssetJdbcRepository imageAssetRepository;

    @Autowired
    UserItemRepository userItemRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private User user;
    private Long userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM image_assets");
        jdbcTemplate.update("DELETE FROM user_items");
        jdbcTemplate.update("DELETE FROM ai_jobs");
        jdbcTemplate.update("DELETE FROM users");

        user = userRepository.saveAndFlush(
                User.local(
                        "account-image@example.com",
                        "탈퇴이미지",
                        Gender.NOT_SPECIFIED
                )
        );
        userId = user.getId();
    }

    @Test
    void accountDataDeletionRetainsImagesAsCleanupQueue() {
        Long itemId = createItem();
        long aiJobId = createAiJob();

        long activeImageId =
                imageAssetRepository
                        .createTemporaryItem(
                                userId,
                                "wear-it/user-items/account-active",
                                "https://example.com/account-active.jpg",
                                "jpg",
                                2048L,
                                1200,
                                900
                        );

        assertThat(
                imageAssetRepository
                        .activateTemporaryForItem(
                                userId,
                                activeImageId,
                                itemId
                        )
        ).isTrue();

        jdbcTemplate.update(
                """
                UPDATE image_assets
                SET ai_job_id = ?
                WHERE id = ?
                """,
                aiJobId,
                activeImageId
        );

        long deletedImageId =
                imageAssetRepository
                        .createTemporaryItem(
                                userId,
                                "wear-it/user-items/account-deleted",
                                "https://example.com/account-deleted.jpg",
                                "jpg",
                                2048L,
                                1200,
                                900
                        );

        assertThat(
                imageAssetRepository.markDeletePending(
                        userId,
                        deletedImageId
                )
        ).isTrue();
        assertThat(
                imageAssetRepository.markDeleted(
                        userId,
                        deletedImageId
                )
        ).isTrue();

        jdbcTemplate.update(
                """
                UPDATE image_assets
                SET user_item_id = ?,
                    ai_job_id = ?
                WHERE id = ?
                """,
                itemId,
                aiJobId,
                deletedImageId
        );

        userDataDeletionService.deleteOwnedData(userId);

        ImageAssetData queued =
                imageAssetRepository
                        .findOwnedItemAsset(
                                userId,
                                activeImageId
                        )
                        .orElseThrow();

        assertThat(queued.status())
                .isEqualTo(
                        ImageAssetStatus.DELETE_PENDING
                );
        assertThat(queued.userItemId()).isNull();
        assertThat(queued.aiJobId()).isNull();

        ImageAssetData alreadyDeleted =
                imageAssetRepository
                        .findOwnedItemAsset(
                                userId,
                                deletedImageId
                        )
                        .orElseThrow();

        assertThat(alreadyDeleted.status())
                .isEqualTo(ImageAssetStatus.DELETED);
        assertThat(alreadyDeleted.userItemId()).isNull();
        assertThat(alreadyDeleted.aiJobId()).isNull();
        assertThat(alreadyDeleted.deletedAt()).isNotNull();

        assertThat(count(
                "SELECT COUNT(*) FROM user_items WHERE user_id = ?"
        )).isZero();

        assertThat(count(
                "SELECT COUNT(*) FROM ai_jobs WHERE user_id = ?"
        )).isZero();

        assertThat(count(
                "SELECT COUNT(*) FROM image_assets WHERE owner_user_id = ?"
        )).isEqualTo(2);
    }

    private Long createItem() {
        UserItem item = userItemRepository.saveAndFlush(
                UserItem.create(
                        user,
                        null,
                        "MCM",
                        "탈퇴 테스트 아이템",
                        ItemCategory.BAG,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        return item.getId();
    }

    private long createAiJob() {
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
                    'SUCCEEDED',
                    'account-delete-image-job',
                    NULL,
                    'test-model',
                    'test-v1',
                    NULL,
                    0,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                userId
        );

        Long id = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM ai_jobs
                WHERE user_id = ?
                  AND idempotency_key =
                      'account-delete-image-job'
                """,
                Long.class,
                userId
        );

        if (id == null) {
            throw new IllegalStateException(
                    "테스트 AI Job 생성에 실패했습니다."
            );
        }

        return id;
    }

    private int count(String sql) {
        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                userId
        );

        return count == null ? 0 : count;
    }
}
