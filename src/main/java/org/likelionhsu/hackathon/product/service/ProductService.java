package org.likelionhsu.hackathon.product.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.likelionhsu.hackathon.cart.repository.CartItemJdbcRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductDetailResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductImageResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductListItemResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductTagsResponse;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTagMapping;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductSpecification;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.likelionhsu.hackathon.wishlist.repository.WishlistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductTagMappingRepository productTagMappingRepository;
    private final WishlistRepository wishlistRepository;
    private final CartItemJdbcRepository cartItemRepository;

    public ProductService(
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ProductTagMappingRepository productTagMappingRepository,
            WishlistRepository wishlistRepository,
            CartItemJdbcRepository cartItemRepository
    ) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productTagMappingRepository = productTagMappingRepository;
        this.wishlistRepository = wishlistRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public PageResponse<ProductListItemResponse> getProducts(
            Long userId,
            ItemCategory category,
            ColorGroup color,
            Long minPrice,
            Long maxPrice,
            Pageable pageable
    ) {
        Specification<Product> specification =
                ProductSpecification
                        .hasStatus(ProductStatus.ACTIVE)
                        .and(
                                ProductSpecification.hasCategory(
                                        category
                                )
                        )
                        .and(
                                ProductSpecification.hasPrimaryColor(
                                        color
                                )
                        )
                        .and(
                                ProductSpecification
                                        .priceGreaterThanOrEqualTo(
                                                minPrice
                                        )
                        )
                        .and(
                                ProductSpecification
                                        .priceLessThanOrEqualTo(
                                                maxPrice
                                        )
                        );

        Page<Product> productPage =
                productRepository.findAll(
                        specification,
                        pageable
                );

        Map<Long, String> primaryImageUrls =
                findPrimaryImageUrls(
                        productPage.getContent()
                );

        Set<Long> favoritedProductIds =
                findFavoritedProductIds(
                        userId,
                        productPage.getContent()
                );

        Page<ProductListItemResponse> responsePage =
                productPage.map(
                        product ->
                                toListItemResponse(
                                        product,
                                        primaryImageUrls.get(
                                                product.getId()
                                        ),
                                        favoritedProductIds.contains(
                                                product.getId()
                                        )
                                )
                );

        return PageResponse.from(responsePage);
    }

    public ProductDetailResponse getProduct(
            Long userId,
            Long productId
    ) {
        Product product =
                productRepository
                        .findByIdAndStatus(
                                productId,
                                ProductStatus.ACTIVE
                        )
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.PRODUCT_NOT_FOUND
                                        )
                        );

        boolean favorited =
                wishlistRepository
                        .existsByUser_IdAndProduct_Id(
                                userId,
                                productId
                        );

        boolean inCart =
                cartItemRepository
                        .existsByUser_IdAndProduct_Id(
                                userId,
                                productId
                        );

        List<ProductImageResponse> images =
                productImageRepository
                        .findAllByProduct_IdOrderBySortOrderAsc(
                                productId
                        )
                        .stream()
                        .map(this::toImageResponse)
                        .toList();

        List<ProductTagMapping> mappings =
                productTagMappingRepository
                        .findAllWithTagByProductId(
                                productId
                        );

        ProductTagsResponse tags =
                toTagsResponse(mappings);

        return new ProductDetailResponse(
                String.valueOf(product.getId()),
                product.getBrand(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getDescription(),
                product.getPrice(),
                product.getPrimaryColor(),
                product.getMaterial(),
                product.getProductUrl(),
                images,
                tags,
                favorited,
                inCart
        );
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

    private Set<Long> findFavoritedProductIds(
            Long userId,
            List<Product> products
    ) {
        if (products.isEmpty()) {
            return Set.of();
        }

        List<Long> productIds =
                products.stream()
                        .map(Product::getId)
                        .toList();

        return wishlistRepository
                .findProductIdsByUserIdAndProductIdIn(
                        userId,
                        productIds
                );
    }

    private ProductListItemResponse toListItemResponse(
            Product product,
            String primaryImageUrl,
            boolean favorited
    ) {
        return new ProductListItemResponse(
                String.valueOf(product.getId()),
                product.getBrand(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getPrimaryColor(),
                primaryImageUrl,
                favorited
        );
    }

    private ProductImageResponse toImageResponse(
            ProductImage image
    ) {
        return new ProductImageResponse(
                image.getUrl(),
                image.getAltText(),
                image.getSortOrder(),
                image.isPrimary()
        );
    }

    private ProductTagsResponse toTagsResponse(
            List<ProductTagMapping> mappings
    ) {
        Map<ProductTagType, List<String>> tagsByType =
                new EnumMap<>(ProductTagType.class);

        for (ProductTagType type : ProductTagType.values()) {
            tagsByType.put(
                    type,
                    new ArrayList<>()
            );
        }

        for (ProductTagMapping mapping : mappings) {
            tagsByType
                    .get(
                            mapping.getProductTag()
                                    .getType()
                    )
                    .add(
                            mapping.getProductTag()
                                    .getCode()
                    );
        }

        return new ProductTagsResponse(
                List.copyOf(
                        tagsByType.get(
                                ProductTagType.STYLE
                        )
                ),
                List.copyOf(
                        tagsByType.get(
                                ProductTagType.SEASON
                        )
                ),
                List.copyOf(
                        tagsByType.get(
                                ProductTagType.OCCASION
                        )
                ),
                List.copyOf(
                        tagsByType.get(
                                ProductTagType.FEATURE
                        )
                )
        );
    }
}