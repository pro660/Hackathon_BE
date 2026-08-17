package org.likelionhsu.hackathon.cart.service;

import org.likelionhsu.hackathon.cart.domain.CartItemData;
import org.likelionhsu.hackathon.cart.dto.response.CartItemResponse;
import org.likelionhsu.hackathon.cart.repository.CartItemJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartItemJdbcRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(
            CartItemJdbcRepository cartItemRepository,
            ProductRepository productRepository
    ) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void addCartItem(
            Long userId,
            Long productId
    ) {
        validateActiveProduct(productId);

        cartItemRepository.insertIfAbsent(
                userId,
                productId
        );
    }

    @Transactional
    public void removeCartItem(
            Long userId,
            Long productId
    ) {
        cartItemRepository
                .deleteByUser_IdAndProduct_Id(
                        userId,
                        productId
                );
    }

    public PageResponse<CartItemResponse> getCartItems(
            Long userId,
            Pageable pageable
    ) {
        Page<CartItemData> cartItemPage =
                cartItemRepository
                        .findAllActiveByUserId(
                                userId,
                                pageable
                        );

        Page<CartItemResponse> responsePage =
                cartItemPage.map(
                        item ->
                                new CartItemResponse(
                                        String.valueOf(
                                                item.cartItemId()
                                        ),
                                        String.valueOf(
                                                item.productId()
                                        ),
                                        item.brand(),
                                        item.name(),
                                        item.price(),
                                        item.primaryImageUrl(),
                                        item.productUrl(),
                                        item.addedAt()
                                )
                );

        return PageResponse.from(responsePage);
    }

    private void validateActiveProduct(
            Long productId
    ) {
        productRepository
                .findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
                .orElseThrow(
                        () -> new BusinessException(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );
    }
}