package org.likelionhsu.hackathon.wishlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;

import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.likelionhsu.hackathon.wishlist.dto.response.WishlistItemResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.likelionhsu.hackathon.wishlist.repository.WishlistRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.likelionhsu.hackathon.product.repository.ProductImageRepository;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    WishlistRepository wishlistRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    UserRepository userRepository;

    WishlistService wishlistService;

    @Mock
    ProductImageRepository productImageRepository;

    @BeforeEach
    void setUp() {
        wishlistService =
                new WishlistService(
                        wishlistRepository,
                        productRepository,
                        userRepository,
                        productImageRepository
                );
    }

    @Test
    void favoriteCanBeAdded() {
        Long userId = 1L;
        Long productId = 10L;

        Product product = product();

        User user = User.local(
                "wishlist@example.com",
                "찜사용자",
                Gender.NOT_SPECIFIED
        );

        when(
                productRepository.findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(Optional.of(product));

        when(
                wishlistRepository
                        .existsByUser_IdAndProduct_Id(
                                userId,
                                productId
                        )
        ).thenReturn(false);

        when(
                userRepository.getReferenceById(userId)
        ).thenReturn(user);

        wishlistService.addFavorite(
                userId,
                productId
        );

        verify(wishlistRepository)
                .save(any(Wishlist.class));
    }

    @Test
    void addingAlreadyFavoritedProductDoesNothing() {
        Long userId = 1L;
        Long productId = 10L;

        when(
                productRepository.findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(Optional.of(product()));

        when(
                wishlistRepository
                        .existsByUser_IdAndProduct_Id(
                                userId,
                                productId
                        )
        ).thenReturn(true);

        wishlistService.addFavorite(
                userId,
                productId
        );

        verify(
                userRepository,
                never()
        ).getReferenceById(any());

        verify(
                wishlistRepository,
                never()
        ).save(any());
    }

    @Test
    void favoriteCanBeRemoved() {
        Long userId = 1L;
        Long productId = 10L;

        when(
                productRepository.findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(Optional.of(product()));

        wishlistService.removeFavorite(
                userId,
                productId
        );

        verify(wishlistRepository)
                .deleteByUser_IdAndProduct_Id(
                        userId,
                        productId
                );
    }

    @Test
    void removingAlreadyUnfavoritedProductStillSucceeds() {
        Long userId = 1L;
        Long productId = 10L;

        when(
                productRepository.findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(Optional.of(product()));

        wishlistService.removeFavorite(
                userId,
                productId
        );

        verify(wishlistRepository)
                .deleteByUser_IdAndProduct_Id(
                        userId,
                        productId
                );
    }

    @Test
    void addingMissingProductThrowsProductNotFound() {
        Long userId = 1L;
        Long productId = 999L;

        when(
                productRepository.findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> wishlistService.addFavorite(
                        userId,
                        productId
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(
                            businessException.getErrorCode()
                    ).isEqualTo(
                            ErrorCode.PRODUCT_NOT_FOUND
                    );
                });

        verify(
                wishlistRepository,
                never()
        ).save(any());
    }

    @Test
    void removingMissingProductThrowsProductNotFound() {
        Long userId = 1L;
        Long productId = 999L;

        when(
                productRepository.findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> wishlistService.removeFavorite(
                        userId,
                        productId
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(
                            businessException.getErrorCode()
                    ).isEqualTo(
                            ErrorCode.PRODUCT_NOT_FOUND
                    );
                });

        verify(
                wishlistRepository,
                never()
        ).deleteByUser_IdAndProduct_Id(
                any(),
                any()
        );
    }

    @Test
    void wishlistsCanBeFetched() {
        Long userId = 1L;

        Pageable pageable =
                PageRequest.of(
                        0,
                        20
                );

        User user = User.local(
                "wishlist-list@example.com",
                "찜목록사용자",
                Gender.NOT_SPECIFIED
        );

        Product firstProduct =
                product(
                        10L,
                        "MCM-WISHLIST-LIST-001",
                        "First Wishlist Bag"
                );

        Product secondProduct =
                product(
                        20L,
                        "MCM-WISHLIST-LIST-002",
                        "Second Wishlist Bag"
                );

        Wishlist firstWishlist =
                Wishlist.create(
                        user,
                        firstProduct
                );

        Wishlist secondWishlist =
                Wishlist.create(
                        user,
                        secondProduct
                );

        when(
                wishlistRepository.findAllByUser_Id(
                        userId,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(
                                firstWishlist,
                                secondWishlist
                        ),
                        pageable,
                        2
                )
        );

        when(
                productImageRepository
                        .findAllByProduct_IdInAndPrimaryTrue(
                                List.of(
                                        10L,
                                        20L
                                )
                        )
        ).thenReturn(
                List.of(
                        ProductImage.create(
                                firstProduct,
                                "https://example.com/first.webp",
                                "first-public-id",
                                "First Wishlist Bag",
                                0,
                                true
                        ),
                        ProductImage.create(
                                secondProduct,
                                "https://example.com/second.webp",
                                "second-public-id",
                                "Second Wishlist Bag",
                                0,
                                true
                        )
                )
        );

        PageResponse<WishlistItemResponse> response =
                wishlistService.getWishlists(
                        userId,
                        pageable
                );

        assertThat(response.page())
                .isZero();

        assertThat(response.size())
                .isEqualTo(20);

        assertThat(response.totalElements())
                .isEqualTo(2);

        assertThat(response.totalPages())
                .isEqualTo(1);

        assertThat(response.items())
                .hasSize(2);

        WishlistItemResponse firstItem =
                response.items().get(0);

        assertThat(firstItem.productId())
                .isEqualTo("10");

        assertThat(firstItem.name())
                .isEqualTo("First Wishlist Bag");

        assertThat(firstItem.primaryImageUrl())
                .isEqualTo(
                        "https://example.com/first.webp"
                );

        assertThat(firstItem.favorited())
                .isTrue();

        WishlistItemResponse secondItem =
                response.items().get(1);

        assertThat(secondItem.productId())
                .isEqualTo("20");

        assertThat(secondItem.primaryImageUrl())
                .isEqualTo(
                        "https://example.com/second.webp"
                );

        assertThat(secondItem.favorited())
                .isTrue();

        verify(wishlistRepository)
                .findAllByUser_Id(
                        userId,
                        pageable
                );

        verify(productImageRepository)
                .findAllByProduct_IdInAndPrimaryTrue(
                        List.of(
                                10L,
                                20L
                        )
                );
    }

    @Test
    void emptyWishlistDoesNotQueryProductImages() {
        Long userId = 1L;

        Pageable pageable =
                PageRequest.of(
                        0,
                        20
                );

        when(
                wishlistRepository.findAllByUser_Id(
                        userId,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(),
                        pageable,
                        0
                )
        );

        PageResponse<WishlistItemResponse> response =
                wishlistService.getWishlists(
                        userId,
                        pageable
                );

        assertThat(response.items())
                .isEmpty();

        assertThat(response.totalElements())
                .isZero();

        verify(
                productImageRepository,
                never()
        ).findAllByProduct_IdInAndPrimaryTrue(
                any()
        );
    }

    private Product product() {
        return Product.create(
                ProductBrand.MCM,
                "MCM-WISHLIST-SERVICE",
                "Wishlist Service Bag",
                ItemCategory.BAG,
                null,
                1_000_000L,
                ColorGroup.BLACK,
                MaterialGroup.LEATHER,
                null,
                ProductStatus.ACTIVE
        );
    }

    private Product product(
            Long id,
            String sku,
            String name
    ) {
        Product product =
                Product.create(
                        ProductBrand.MCM,
                        sku,
                        name,
                        ItemCategory.BAG,
                        null,
                        1_000_000L,
                        ColorGroup.BLACK,
                        MaterialGroup.LEATHER,
                        null,
                        ProductStatus.ACTIVE
                );

        ReflectionTestUtils.setField(
                product,
                "id",
                id
        );

        return product;
    }
}