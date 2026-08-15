package org.likelionhsu.hackathon.wishlist.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.wishlist.dto.response.WishlistItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.likelionhsu.hackathon.wishlist.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductImageRepository productImageRepository;

    public WishlistService(
            WishlistRepository wishlistRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            ProductImageRepository productImageRepository
    ) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productImageRepository = productImageRepository;
    }

    @Transactional
    public void addFavorite(
            Long userId,
            Long productId
    ) {
        Product product = getActiveProduct(productId);

        if (wishlistRepository
                .existsByUser_IdAndProduct_Id(
                        userId,
                        productId
                )) {
            return;
        }

        User user =
                userRepository.getReferenceById(
                        userId
                );

        wishlistRepository.save(
                Wishlist.create(
                        user,
                        product
                )
        );
    }

    @Transactional
    public void removeFavorite(
            Long userId,
            Long productId
    ) {
        getActiveProduct(productId);

        wishlistRepository
                .deleteByUser_IdAndProduct_Id(
                        userId,
                        productId
                );
    }

    private Product getActiveProduct(
            Long productId
    ) {
        return productRepository
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

    public PageResponse<WishlistItemResponse> getWishlists(
            Long userId,
            Pageable pageable
    ) {
        Page<Wishlist> wishlistPage =
                wishlistRepository.findAllByUser_Id(
                        userId,
                        pageable
                );

        List<Product> products =
                wishlistPage
                        .getContent()
                        .stream()
                        .map(Wishlist::getProduct)
                        .toList();

        Map<Long, String> primaryImageUrls =
                findPrimaryImageUrls(products);

        Page<WishlistItemResponse> responsePage =
                wishlistPage.map(
                        wishlist -> {
                            Product product =
                                    wishlist.getProduct();

                            return new WishlistItemResponse(
                                    String.valueOf(
                                            product.getId()
                                    ),
                                    product.getBrand(),
                                    product.getName(),
                                    product.getCategory(),
                                    product.getPrice(),
                                    product.getPrimaryColor(),
                                    primaryImageUrls.get(
                                            product.getId()
                                    ),
                                    true
                            );
                        }
                );

        return PageResponse.from(responsePage);
    }

    private Map<Long, String> findPrimaryImageUrls(
            List<Product> products
    ) {
        if (products.isEmpty()) {
            return Map.of();
        }

        List<Long> productIds =
                products.stream()
                        .map(Product::getId)
                        .toList();

        return productImageRepository
                .findAllByProduct_IdInAndPrimaryTrue(
                        productIds
                )
                .stream()
                .collect(
                        Collectors.toMap(
                                image ->
                                        image.getProduct()
                                                .getId(),
                                ProductImage::getUrl,
                                (first, second) -> first
                        )
                );
    }
}