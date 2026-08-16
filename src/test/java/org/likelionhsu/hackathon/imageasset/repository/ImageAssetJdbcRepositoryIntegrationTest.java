package org.likelionhsu.hackathon.imageasset.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetPurpose;
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
class ImageAssetJdbcRepositoryIntegrationTest {

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
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM image_assets");
        jdbcTemplate.update("DELETE FROM users");

        userId = createUser(
                "image-owner@example.com",
                "이미지소유자"
        );
        otherUserId = createUser(
                "other-image-owner@example.com",
                "다른사용자"
        );
    }

    @Test
    void temporaryItemAssetCanBeCreated() {
        long imageAssetId = createTemporaryItemAsset();

        ImageAssetData asset = repository
                .findOwnedItemAsset(userId, imageAssetId)
                .orElseThrow();

        assertThat(asset.id()).isEqualTo(imageAssetId);
        assertThat(asset.ownerUserId()).isEqualTo(userId);
        assertThat(asset.purpose())
                .isEqualTo(ImageAssetPurpose.ITEM);
        assertThat(asset.status())
                .isEqualTo(ImageAssetStatus.TEMPORARY);
        assertThat(asset.publicId())
                .isEqualTo("wear-it/user-items/test-image");
        assertThat(asset.secureUrl())
                .isEqualTo("https://example.com/test-image.jpg");
        assertThat(asset.format()).isEqualTo("jpg");
        assertThat(asset.bytes()).isEqualTo(2048L);
        assertThat(asset.width()).isEqualTo(1200);
        assertThat(asset.height()).isEqualTo(900);
        assertThat(asset.sortOrder()).isZero();
        assertThat(asset.createdAt()).isNotNull();
    }

    @Test
    void temporaryAssetStartsUnlinked() {
        long imageAssetId = createTemporaryItemAsset();

        ImageAssetData asset = repository
                .findOwnedItemAsset(userId, imageAssetId)
                .orElseThrow();

        assertThat(asset.userItemId()).isNull();
        assertThat(asset.aiJobId()).isNull();
        assertThat(asset.activatedAt()).isNull();
        assertThat(asset.deletedAt()).isNull();
    }

    @Test
    void ownedItemAssetCanBeFound() {
        long imageAssetId = createTemporaryItemAsset();

        Optional<ImageAssetData> found =
                repository.findOwnedItemAsset(
                        userId,
                        imageAssetId
                );

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().id())
                .isEqualTo(imageAssetId);
    }

    @Test
    void anotherUsersImageIsHidden() {
        long imageAssetId = createTemporaryItemAsset();

        Optional<ImageAssetData> found =
                repository.findOwnedItemAsset(
                        otherUserId,
                        imageAssetId
                );

        assertThat(found).isEmpty();
    }

    @Test
    void temporaryStatusWorksOnExistingV5Schema() {
        long imageAssetId = createTemporaryItemAsset();

        String status = jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM image_assets
                WHERE id = ?
                """,
                String.class,
                imageAssetId
        );

        assertThat(status).isEqualTo("TEMPORARY");
    }

    @Test
    void temporaryAssetCanBeMarkedDeletePending() {
        long imageAssetId = createTemporaryItemAsset();

        boolean updated = repository.markDeletePending(
                userId,
                imageAssetId
        );

        assertThat(updated).isTrue();

        ImageAssetData asset = repository
                .findOwnedItemAsset(userId, imageAssetId)
                .orElseThrow();

        assertThat(asset.status())
                .isEqualTo(ImageAssetStatus.DELETE_PENDING);
        assertThat(asset.deletedAt()).isNull();
    }

    @Test
    void anotherUserCannotMarkDeletePending() {
        long imageAssetId = createTemporaryItemAsset();

        boolean updated = repository.markDeletePending(
                otherUserId,
                imageAssetId
        );

        assertThat(updated).isFalse();

        ImageAssetData asset = repository
                .findOwnedItemAsset(userId, imageAssetId)
                .orElseThrow();

        assertThat(asset.status())
                .isEqualTo(ImageAssetStatus.TEMPORARY);
    }

    @Test
    void deletePendingAssetCanBeMarkedDeleted() {
        long imageAssetId = createTemporaryItemAsset();

        assertThat(
                repository.markDeletePending(
                        userId,
                        imageAssetId
                )
        ).isTrue();

        boolean deleted = repository.markDeleted(
                userId,
                imageAssetId
        );

        assertThat(deleted).isTrue();

        ImageAssetData asset = repository
                .findOwnedItemAsset(userId, imageAssetId)
                .orElseThrow();

        assertThat(asset.status())
                .isEqualTo(ImageAssetStatus.DELETED);
        assertThat(asset.deletedAt()).isNotNull();
    }

    @Test
    void temporaryAssetCannotBeMarkedDeletedDirectly() {
        long imageAssetId = createTemporaryItemAsset();

        boolean deleted = repository.markDeleted(
                userId,
                imageAssetId
        );

        assertThat(deleted).isFalse();

        ImageAssetData asset = repository
                .findOwnedItemAsset(userId, imageAssetId)
                .orElseThrow();

        assertThat(asset.status())
                .isEqualTo(ImageAssetStatus.TEMPORARY);
    }

    private long createTemporaryItemAsset() {
        return repository.createTemporaryItem(
                userId,
                "wear-it/user-items/test-image",
                "https://example.com/test-image.jpg",
                "jpg",
                2048L,
                1200,
                900
        );
    }

    private Long createUser(
            String email,
            String nickname
    ) {
        User user = userRepository.saveAndFlush(
                User.local(
                        email,
                        nickname,
                        Gender.NOT_SPECIFIED
                )
        );

        return user.getId();
    }
}
