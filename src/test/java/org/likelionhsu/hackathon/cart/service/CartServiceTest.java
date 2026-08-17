package org.likelionhsu.hackathon.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.cart.domain.CartItemData;
import org.likelionhsu.hackathon.cart.dto.response.CartItemResponse;
import org.likelionhsu.hackathon.cart.repository.CartItemJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemJdbcRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService =
                new CartService(
                        cartItemRepository,
                        productRepository
                );
    }

    @Test
    void addCartItemStoresActiveProduct() {
        Product product =
                org.mockito.Mockito.mock(Product.class);

        when(
                productRepository.findByIdAndStatus(
                        10L,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(Optional.of(product));

        cartService.addCartItem(1L, 10L);

        verify(cartItemRepository)
                .insertIfAbsent(
                        1L,
                        10L
                );
    }

    @Test
    void addCartItemAcceptsDuplicateAsSuccessfulNoOp() {
        Product product =
                org.mockito.Mockito.mock(Product.class);

        when(
                productRepository.findByIdAndStatus(
                        10L,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(Optional.of(product));

        when(
                cartItemRepository.insertIfAbsent(
                        1L,
                        10L
                )
        ).thenReturn(0);

        cartService.addCartItem(1L, 10L);

        verify(cartItemRepository)
                .insertIfAbsent(
                        1L,
                        10L
                );
    }

    @Test
    void addCartItemRejectsMissingOrInactiveProduct() {
        when(
                productRepository.findByIdAndStatus(
                        999L,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> cartService.addCartItem(1L, 999L)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(
                            businessException.getErrorCode()
                    ).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                });

        verify(
                cartItemRepository,
                never()
        ).insertIfAbsent(any(), any());
    }

    @Test
    void removeCartItemIsIdempotentAndDoesNotRequireActiveProduct() {
        cartService.removeCartItem(1L, 10L);

        verify(cartItemRepository)
                .deleteByUser_IdAndProduct_Id(
                        1L,
                        10L
                );

        verify(
                productRepository,
                never()
        ).findByIdAndStatus(any(), any());
    }

    @Test
    void getCartItemsUsesCurrentCatalogData() {
        Pageable pageable =
                PageRequest.of(0, 20);

        Instant addedAt =
                Instant.parse(
                        "2026-08-18T00:00:00Z"
                );

        CartItemData data =
                new CartItemData(
                        30L,
                        10L,
                        ProductBrand.MCM,
                        "MCM 테스트 백",
                        1_490_000L,
                        "https://example.com/mcm.webp",
                        "https://kr.mcmworldwide.com/test",
                        addedAt
                );

        when(
                cartItemRepository
                        .findAllActiveByUserId(
                                1L,
                                pageable
                        )
        ).thenReturn(
                new PageImpl<>(
                        List.of(data),
                        pageable,
                        1
                )
        );

        PageResponse<CartItemResponse> response =
                cartService.getCartItems(
                        1L,
                        pageable
                );

        assertThat(response.items())
                .hasSize(1);

        CartItemResponse item =
                response.items().getFirst();

        assertThat(item.cartItemId())
                .isEqualTo("30");
        assertThat(item.productId())
                .isEqualTo("10");
        assertThat(item.brand())
                .isEqualTo(ProductBrand.MCM);
        assertThat(item.name())
                .isEqualTo("MCM 테스트 백");
        assertThat(item.price())
                .isEqualTo(1_490_000L);
        assertThat(item.primaryImageUrl())
                .isEqualTo(
                        "https://example.com/mcm.webp"
                );
        assertThat(item.productUrl())
                .isEqualTo(
                        "https://kr.mcmworldwide.com/test"
                );
        assertThat(item.addedAt())
                .isEqualTo(addedAt);
        assertThat(response.totalElements())
                .isEqualTo(1);
    }

    @Test
    void emptyCartReturnsEmptyPage() {
        Pageable pageable =
                PageRequest.of(0, 20);

        when(
                cartItemRepository
                        .findAllActiveByUserId(
                                1L,
                                pageable
                        )
        ).thenReturn(
                new PageImpl<>(
                        List.of(),
                        pageable,
                        0
                )
        );

        PageResponse<CartItemResponse> response =
                cartService.getCartItems(
                        1L,
                        pageable
                );

        assertThat(response.items())
                .isEmpty();
    }
}