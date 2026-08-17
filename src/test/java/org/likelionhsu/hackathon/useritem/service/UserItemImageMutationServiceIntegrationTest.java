package org.likelionhsu.hackathon.useritem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisInputHasher;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
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
class UserItemImageMutationServiceIntegrationTest {

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
    UserItemImageMutationService mutationService;

    @Autowired
    UserItemImageRepository userItemImageRepository;

    @Autowired
    ImageAssetJdbcRepository imageAssetRepository;

    @Autowired
    ItemAnalysisInputHasher itemAnalysisInputHasher;

    @Autowired
    UserItemRepository userItemRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TransactionTemplate transactionTemplate;

    private ExecutorService executor;

    private User owner;
    private User otherUser;
    private Long itemId;
    private Long otherItemId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM image_assets");
        jdbcTemplate.update("DELETE FROM user_items");
        jdbcTemplate.update("DELETE FROM ai_jobs");
        jdbcTemplate.update("DELETE FROM users");

        owner = createUser(
                "item-image-owner@example.com",
                "이미지소유자"
        );
        otherUser = createUser(
                "other-item-image@example.com",
                "다른사용자"
        );

        itemId = createItem(owner, "소유 아이템");
        otherItemId =
                createItem(owner, "다른 소유 아이템");

        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void temporaryImageCanBeAttached() {
        long imageId =
                createTemporaryImage(
                        owner.getId(),
                        "attach-first"
                );

        Long versionBefore = currentItemVersion(itemId);

        UserItemImageMutationService.AttachMutation
                result = mutationService.attach(
                owner.getId(),
                itemId,
                imageId
        );

        ImageAssetData image = ownedAsset(
                owner.getId(),
                imageId
        );

        assertThat(result.response().imageAssetId())
                .isEqualTo(String.valueOf(imageId));
        assertThat(image.status())
                .isEqualTo(ImageAssetStatus.ACTIVE);
        assertThat(image.userItemId())
                .isEqualTo(itemId);
        assertThat(image.activatedAt()).isNotNull();
        assertThat(activeCount(itemId)).isEqualTo(1);
        assertThat(currentItemVersion(itemId))
                .isEqualTo(versionBefore);
    }

    @Test
    void replacementLeavesOnlyNewImageActive() {
        long oldImageId =
                createTemporaryImage(
                        owner.getId(),
                        "replace-old"
                );
        long newImageId =
                createTemporaryImage(
                        owner.getId(),
                        "replace-new"
                );

        mutationService.attach(
                owner.getId(),
                itemId,
                oldImageId
        );

        UserItemImageMutationService.AttachMutation
                result = mutationService.attach(
                owner.getId(),
                itemId,
                newImageId
        );

        assertThat(result.cleanupTargets())
                .extracting(ImageAssetData::id)
                .containsExactly(oldImageId);

        assertThat(
                ownedAsset(
                        owner.getId(),
                        oldImageId
                ).status()
        ).isEqualTo(
                ImageAssetStatus.DELETE_PENDING
        );

        ImageAssetData newImage = ownedAsset(
                owner.getId(),
                newImageId
        );

        assertThat(newImage.status())
                .isEqualTo(ImageAssetStatus.ACTIVE);
        assertThat(newImage.userItemId())
                .isEqualTo(itemId);
        assertThat(activeCount(itemId))
                .isEqualTo(1);
    }

    @Test
    void repeatedSameAttachIsIdempotent() {
        long imageId =
                createTemporaryImage(
                        owner.getId(),
                        "attach-idempotent"
                );

        mutationService.attach(
                owner.getId(),
                itemId,
                imageId
        );

        ImageAssetData first = ownedAsset(
                owner.getId(),
                imageId
        );

        UserItemImageMutationService.AttachMutation
                second = mutationService.attach(
                owner.getId(),
                itemId,
                imageId
        );

        ImageAssetData after = ownedAsset(
                owner.getId(),
                imageId
        );

        assertThat(second.cleanupTargets()).isEmpty();
        assertThat(after.activatedAt())
                .isEqualTo(first.activatedAt());
        assertThat(activeCount(itemId))
                .isEqualTo(1);
    }

    @Test
    void activeImageCannotMoveToAnotherItem() {
        long imageId =
                createTemporaryImage(
                        owner.getId(),
                        "active-other-item"
                );

        mutationService.attach(
                owner.getId(),
                itemId,
                imageId
        );

        assertBusinessError(
                () -> mutationService.attach(
                        owner.getId(),
                        otherItemId,
                        imageId
                ),
                ErrorCode.IMAGE_ASSET_STATE_CONFLICT
        );
    }

    @Test
    void otherUsersImageIsHiddenAs404() {
        long imageId =
                createTemporaryImage(
                        otherUser.getId(),
                        "other-owner"
                );

        assertBusinessError(
                () -> mutationService.attach(
                        owner.getId(),
                        itemId,
                        imageId
                ),
                ErrorCode.IMAGE_ASSET_NOT_FOUND
        );
    }

    @Test
    void linkedDeleteKeepsItemHistoryAndBecomesPending() {
        long imageId =
                createTemporaryImage(
                        owner.getId(),
                        "linked-delete"
                );

        mutationService.attach(
                owner.getId(),
                itemId,
                imageId
        );

        UserItemImageMutationService.DeleteMutation
                result =
                mutationService.deleteLinkedImage(
                        owner.getId(),
                        itemId,
                        imageId
                );

        ImageAssetData image = ownedAsset(
                owner.getId(),
                imageId
        );

        assertThat(result.cleanupTarget()).isNotNull();
        assertThat(image.status())
                .isEqualTo(
                        ImageAssetStatus.DELETE_PENDING
                );
        assertThat(image.userItemId())
                .isEqualTo(itemId);
        assertThat(activeCount(itemId)).isZero();

        UserItemImageMutationService.DeleteMutation
                repeated =
                mutationService.deleteLinkedImage(
                        owner.getId(),
                        itemId,
                        imageId
                );

        assertThat(repeated.cleanupTarget()).isNotNull();
    }

    @Test
    void analyzedImageCanBeAttachedAsFirstImage() {
        long imageId =
                createTemporaryImage(
                        owner.getId(),
                        "analysis-match"
                );

        ImageAssetData image =
                ownedAsset(
                        owner.getId(),
                        imageId
                );

        String inputHash =
                itemAnalysisInputHasher.hash(image);

        Long aiJobId = createAiJob(
                owner.getId(),
                "SUCCEEDED",
                inputHash,
                validAnalysisResultJson()
        );

        boolean bound =
                imageAssetRepository
                        .bindAiJobToTemporaryItemAsset(
                                owner.getId(),
                                imageId,
                                aiJobId
                        );

        assertThat(bound).isTrue();

        Long analyzedItemId =
                createItem(
                        owner,
                        "AI 분석 아이템",
                        aiJobId
                );

        mutationService.attach(
                owner.getId(),
                analyzedItemId,
                imageId
        );

        ImageAssetData attached =
                ownedAsset(
                        owner.getId(),
                        imageId
                );

        assertThat(attached.status())
                .isEqualTo(ImageAssetStatus.ACTIVE);
        assertThat(attached.userItemId())
                .isEqualTo(analyzedItemId);
    }

    @Test
    void differentFirstImageIsRejectedForAnalyzedItem() {
        long analyzedImageId =
                createTemporaryImage(
                        owner.getId(),
                        "analysis-origin"
                );

        ImageAssetData analyzedImage =
                ownedAsset(
                        owner.getId(),
                        analyzedImageId
                );

        Long aiJobId = createAiJob(
                owner.getId(),
                "SUCCEEDED",
                itemAnalysisInputHasher.hash(
                        analyzedImage
                ),
                validAnalysisResultJson()
        );

        boolean bound =
                imageAssetRepository
                        .bindAiJobToTemporaryItemAsset(
                                owner.getId(),
                                analyzedImageId,
                                aiJobId
                        );

        assertThat(bound).isTrue();

        Long analyzedItemId =
                createItem(
                        owner,
                        "AI 불일치 아이템",
                        aiJobId
                );

        long differentImageId =
                createTemporaryImage(
                        owner.getId(),
                        "analysis-different"
                );

        assertBusinessError(
                () -> mutationService.attach(
                        owner.getId(),
                        analyzedItemId,
                        differentImageId
                ),
                ErrorCode.IMAGE_ASSET_ANALYSIS_MISMATCH
        );

        assertThat(
                ownedAsset(
                        owner.getId(),
                        differentImageId
                ).status()
        ).isEqualTo(ImageAssetStatus.TEMPORARY);
    }

    @Test
    void newerImageAiJobBindingDoesNotReplaceItemProvenance() {
        long imageId =
                createTemporaryImage(
                        owner.getId(),
                        "analysis-rebound"
                );

        ImageAssetData image =
                ownedAsset(
                        owner.getId(),
                        imageId
                );

        String inputHash =
                itemAnalysisInputHasher.hash(image);

        Long originalJobId = createAiJob(
                owner.getId(),
                "SUCCEEDED",
                inputHash,
                validAnalysisResultJson()
        );

        boolean firstBound =
                imageAssetRepository
                        .bindAiJobToTemporaryItemAsset(
                                owner.getId(),
                                imageId,
                                originalJobId
                        );

        assertThat(firstBound).isTrue();

        Long analyzedItemId =
                createItem(
                        owner,
                        "AI provenance 고정",
                        originalJobId
                );

        Long newerJobId = createAiJob(
                owner.getId(),
                "SUCCEEDED",
                inputHash,
                validAnalysisResultJson()
        );

        boolean rebound =
                imageAssetRepository
                        .bindAiJobToTemporaryItemAsset(
                                owner.getId(),
                                imageId,
                                newerJobId
                        );

        assertThat(rebound).isTrue();

        assertThat(
                ownedAsset(
                        owner.getId(),
                        imageId
                ).aiJobId()
        ).isEqualTo(newerJobId);

        mutationService.attach(
                owner.getId(),
                analyzedItemId,
                imageId
        );

        assertThat(
                ownedAsset(
                        owner.getId(),
                        imageId
                ).status()
        ).isEqualTo(ImageAssetStatus.ACTIVE);
    }

    @Test
    void imageUsedByRunningAnalysisCannotBeAttached() {
        long imageId =
                createTemporaryImage(
                        owner.getId(),
                        "analysis-running"
                );

        ImageAssetData image =
                ownedAsset(
                        owner.getId(),
                        imageId
                );

        Long runningJobId = createAiJob(
                owner.getId(),
                "PENDING",
                itemAnalysisInputHasher.hash(image),
                null
        );

        boolean bound =
                imageAssetRepository
                        .bindAiJobToTemporaryItemAsset(
                                owner.getId(),
                                imageId,
                                runningJobId
                        );

        assertThat(bound).isTrue();

        assertBusinessError(
                () -> mutationService.attach(
                        owner.getId(),
                        itemId,
                        imageId
                ),
                ErrorCode.IMAGE_ASSET_IN_USE
        );

        assertThat(
                ownedAsset(
                        owner.getId(),
                        imageId
                ).status()
        ).isEqualTo(ImageAssetStatus.TEMPORARY);
    }

    @Test
    void deletedImageHistoryMakesNextAttachAReplacement() {
        long firstImageId =
                createTemporaryImage(
                        owner.getId(),
                        "history-first"
                );

        ImageAssetData firstImage =
                ownedAsset(
                        owner.getId(),
                        firstImageId
                );

        Long aiJobId = createAiJob(
                owner.getId(),
                "SUCCEEDED",
                itemAnalysisInputHasher.hash(firstImage),
                validAnalysisResultJson()
        );

        boolean bound =
                imageAssetRepository
                        .bindAiJobToTemporaryItemAsset(
                                owner.getId(),
                                firstImageId,
                                aiJobId
                        );

        assertThat(bound).isTrue();

        Long analyzedItemId =
                createItem(
                        owner,
                        "삭제 이력 아이템",
                        aiJobId
                );

        mutationService.attach(
                owner.getId(),
                analyzedItemId,
                firstImageId
        );

        mutationService.deleteLinkedImage(
                owner.getId(),
                analyzedItemId,
                firstImageId
        );

        boolean deleted =
                imageAssetRepository.markDeleted(
                        owner.getId(),
                        firstImageId
                );

        assertThat(deleted).isTrue();

        assertThat(
                ownedAsset(
                        owner.getId(),
                        firstImageId
                ).status()
        ).isEqualTo(ImageAssetStatus.DELETED);

        long replacementImageId =
                createTemporaryImage(
                        owner.getId(),
                        "history-replacement"
                );

        mutationService.attach(
                owner.getId(),
                analyzedItemId,
                replacementImageId
        );

        ImageAssetData replacement =
                ownedAsset(
                        owner.getId(),
                        replacementImageId
                );

        assertThat(replacement.status())
                .isEqualTo(ImageAssetStatus.ACTIVE);
        assertThat(replacement.userItemId())
                .isEqualTo(analyzedItemId);
    }

    @Test
    void itemLockContentionReturnsConflictAndKeepsOneActive()
            throws Exception {
        long firstImageId =
                createTemporaryImage(
                        owner.getId(),
                        "concurrent-first"
                );
        long secondImageId =
                createTemporaryImage(
                        owner.getId(),
                        "concurrent-second"
                );

        CountDownLatch itemLocked =
                new CountDownLatch(1);
        CountDownLatch releaseLock =
                new CountDownLatch(1);

        Future<UserItemImageMutationService.AttachMutation>
                successfulAttach = executor.submit(() ->
                transactionTemplate.execute(status -> {
                    boolean locked =
                            userItemImageRepository
                                    .lockOwnedActiveItem(
                                            owner.getId(),
                                            itemId
                                    );

                    if (!locked) {
                        throw new IllegalStateException(
                                "테스트 UserItem 잠금에 실패했습니다."
                        );
                    }

                    itemLocked.countDown();

                    await(
                            releaseLock,
                            Duration.ofSeconds(5)
                    );

                    return mutationService.attach(
                            owner.getId(),
                            itemId,
                            firstImageId
                    );
                })
        );

        assertThat(
                itemLocked.await(
                        5,
                        TimeUnit.SECONDS
                )
        ).isTrue();

        try {
            assertBusinessError(
                    () -> mutationService.attach(
                            owner.getId(),
                            itemId,
                            secondImageId
                    ),
                    ErrorCode
                            .IMAGE_ASSET_STATE_CONFLICT
            );
        } finally {
            releaseLock.countDown();
        }

        UserItemImageMutationService.AttachMutation
                result = successfulAttach.get(
                5,
                TimeUnit.SECONDS
        );

        assertThat(result.response().imageAssetId())
                .isEqualTo(
                        String.valueOf(firstImageId)
                );
        assertThat(activeCount(itemId))
                .isEqualTo(1);
    }

    private User createUser(
            String email,
            String nickname
    ) {
        return userRepository.saveAndFlush(
                User.local(
                        email,
                        nickname,
                        Gender.NOT_SPECIFIED
                )
        );
    }

    private Long createItem(
            User user,
            String name
    ) {
        return createItem(
                user,
                name,
                null
        );
    }

    private Long createItem(
            User user,
            String name,
            Long aiJobId
    ) {
        UserItem item = userItemRepository.saveAndFlush(
                UserItem.create(
                        user,
                        null,
                        "MCM",
                        name,
                        ItemCategory.BAG,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        aiJobId,
                        null
                )
        );

        return item.getId();
    }

    private Long createAiJob(
            Long userId,
            String status,
            String inputHash,
            String resultJson
    ) {
        String idempotencyKey =
                "integration-"
                        + status
                        + "-"
                        + System.nanoTime();

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
                    result_json,
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
                    'integration-test',
                    'v1',
                    ?,
                    ?,
                    0,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                userId,
                status,
                idempotencyKey,
                inputHash,
                resultJson
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
                idempotencyKey
        );
    }

    private String validAnalysisResultJson() {
        return """
                {
                  "brandName":"MCM",
                  "name":"통합 테스트 아이템",
                  "category":"BAG",
                  "primaryColor":null,
                  "material":null
                }
                """;
    }

    private long createTemporaryImage(
            Long userId,
            String suffix
    ) {
        return imageAssetRepository.createTemporaryItem(
                userId,
                "wear-it/user-items/" + suffix,
                "https://example.com/"
                        + suffix
                        + ".jpg",
                "jpg",
                2048L,
                1200,
                900
        );
    }

    private ImageAssetData ownedAsset(
            Long userId,
            Long imageId
    ) {
        return imageAssetRepository
                .findOwnedItemAsset(
                        userId,
                        imageId
                )
                .orElseThrow();
    }

    private int activeCount(Long userItemId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM image_assets
                WHERE user_item_id = ?
                  AND purpose = 'ITEM'
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                """,
                Integer.class,
                userItemId
        );

        return count == null ? 0 : count;
    }

    private Long currentItemVersion(Long userItemId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM user_items
                WHERE id = ?
                """,
                Long.class,
                userItemId
        );
    }

    private void assertBusinessError(
            ThrowingAction action,
            ErrorCode expected
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(expected)
                );
    }

    private void await(
            CountDownLatch latch,
            Duration timeout
    ) {
        try {
            boolean completed = latch.await(
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (!completed) {
                throw new IllegalStateException(
                        "동시성 테스트 대기 시간이 초과되었습니다."
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "동시성 테스트가 중단되었습니다.",
                    exception
            );
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {

        void run();
    }
}
