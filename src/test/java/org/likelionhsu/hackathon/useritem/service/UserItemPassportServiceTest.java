package org.likelionhsu.hackathon.useritem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemCreateRequest;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemUpdateRequest;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemDetailResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemPassportResponse;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemAiJobValidator;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageData;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserItemPassportServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant NOW =
            Instant.parse("2026-08-18T00:00:00Z");

    @Mock
    UserItemRepository userItemRepository;
    @Mock
    UserItemImageRepository userItemImageRepository;
    @Mock
    UserItemAiJobValidator userItemAiJobValidator;
    @Mock
    UserRepository userRepository;
    @Mock
    ProductRepository productRepository;

    UserItemService userItemService;

    @BeforeEach
    void setUp() {
        userItemService = new UserItemService(
                userItemRepository,
                userItemImageRepository,
                userItemAiJobValidator,
                userRepository,
                productRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void passportReturnsOwnedItemProductAndPurchaseInfo() {
        Product product = product(20L);
        UserItem item = item(10L, product);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(10L, USER_ID))
                .thenReturn(Optional.of(item));
        when(userItemImageRepository.findActiveImages(USER_ID, 10L))
                .thenReturn(List.of(
                        new UserItemImageData(
                                100L,
                                10L,
                                "https://example.com/item.webp",
                                0
                        )
                ));

        UserItemPassportResponse response =
                userItemService.getMyItemPassport(USER_ID, 10L);

        assertThat(response.myItemId()).isEqualTo("10");
        assertThat(response.productInfo().linkedProductId())
                .isEqualTo("20");
        assertThat(response.productInfo().brandName()).isEqualTo("MCM");
        assertThat(response.productInfo().name()).isEqualTo("내 가방");
        assertThat(response.productInfo().imageUrl())
                .isEqualTo("https://example.com/item.webp");
        assertThat(response.productInfo().sku()).isEqualTo("SKU-20");
        assertThat(response.productInfo().productUrl())
                .isEqualTo("https://example.com/products/20");
        assertThat(response.purchaseInfo().purchaseOrderNumber())
                .isEqualTo("ORDER-2026-001");
        assertThat(response.purchaseInfo().purchaseDate())
                .isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.purchaseInfo().purchasePrice())
                .isEqualTo(1_500_000L);
        assertThat(response.purchaseInfo().purchasePlace())
                .isEqualTo("MCM 청담점");
    }

    @Test
    void passportWorksWithoutLinkedProductOrImage() {
        UserItem item = item(10L, null);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(10L, USER_ID))
                .thenReturn(Optional.of(item));
        when(userItemImageRepository.findActiveImages(USER_ID, 10L))
                .thenReturn(List.of());

        UserItemPassportResponse response =
                userItemService.getMyItemPassport(USER_ID, 10L);

        assertThat(response.productInfo().linkedProductId()).isNull();
        assertThat(response.productInfo().sku()).isNull();
        assertThat(response.productInfo().productUrl()).isNull();
        assertThat(response.productInfo().imageUrl()).isNull();
    }

    @Test
    void passportHidesAnotherUsersOrDeletedItemAsNotFound() {
        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(999L, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> userItemService.getMyItemPassport(USER_ID, 999L)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(ErrorCode.MY_ITEM_NOT_FOUND)
                );
    }

    @Test
    void purchaseMetadataIsNormalizedOnCreate() {
        when(userRepository.getReferenceById(USER_ID))
                .thenReturn(user());
        when(userItemRepository.save(any(UserItem.class)))
                .thenAnswer(invocation -> {
                    UserItem item = invocation.getArgument(0);
                    ReflectionTestUtils.setField(item, "id", 10L);
                    return item;
                });

        userItemService.createMyItem(
                USER_ID,
                new UserItemCreateRequest(
                        null,
                        "MCM",
                        "내 가방",
                        ItemCategory.BAG,
                        ColorGroup.BLACK,
                        MaterialGroup.LEATHER,
                        MaterialSource.USER_CONFIRMED,
                        LocalDate.of(2026, 6, 1),
                        1_500_000L,
                        "  ORDER-2026-001  ",
                        "  MCM 청담점  ",
                        null,
                        null,
                        null
                )
        );

        verify(userItemRepository).save(
                argThat(item ->
                        "ORDER-2026-001".equals(
                                item.getPurchaseOrderNumber()
                        )
                                && "MCM 청담점".equals(
                                item.getPurchasePlace()
                        )
                )
        );
    }

    @Test
    void explicitNullClearsPurchaseMetadataOnPatch() {
        UserItem item = item(10L, null);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(10L, USER_ID))
                .thenReturn(Optional.of(item));
        when(userItemImageRepository.findActiveImages(USER_ID, 10L))
                .thenReturn(List.of());

        UserItemUpdateRequest request = new UserItemUpdateRequest();
        request.setPurchaseOrderNumber(null);
        request.setPurchasePlace(null);
        request.setVersion(2L);

        UserItemDetailResponse response =
                userItemService.updateMyItem(USER_ID, 10L, request);

        assertThat(response.purchaseOrderNumber()).isNull();
        assertThat(response.purchasePlace()).isNull();
        verify(userItemRepository).saveAndFlush(item);
    }

    private UserItem item(Long id, Product product) {
        UserItem item = UserItem.create(
                user(),
                product,
                "MCM",
                "내 가방",
                ItemCategory.BAG,
                ColorGroup.BLACK,
                MaterialGroup.LEATHER,
                MaterialSource.USER_CONFIRMED,
                LocalDate.of(2026, 6, 1),
                1_500_000L,
                "ORDER-2026-001",
                "MCM 청담점",
                null,
                null,
                null
        );

        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "version", 2L);
        ReflectionTestUtils.setField(item, "createdAt", NOW);
        ReflectionTestUtils.setField(item, "updatedAt", NOW);
        return item;
    }

    private Product product(Long id) {
        Product product = Product.create(
                ProductBrand.MCM,
                "SKU-" + id,
                "카탈로그 제품명",
                ItemCategory.BAG,
                null,
                2_000_000L,
                ColorGroup.BLACK,
                MaterialGroup.LEATHER,
                "https://example.com/products/" + id,
                ProductStatus.ACTIVE
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private User user() {
        User user = User.local(
                "passport@example.com",
                "패스포트사용자",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
