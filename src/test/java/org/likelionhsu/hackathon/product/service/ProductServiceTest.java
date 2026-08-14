package org.likelionhsu.hackathon.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductDetailResponse;
import org.likelionhsu.hackathon.product.dto.response.ProductListItemResponse;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTag;
import org.likelionhsu.hackathon.product.entity.ProductTagMapping;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductTagMappingRepository productTagMappingRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService =
                new ProductService(
                        productRepository,
                        productImageRepository,
                        productTagMappingRepository
                );
    }

    @Test
    void productListMapsPrimaryImage() {
        Product product =
                listProduct(
                        1L,
                        "MCM Black Bag"
                );

        ProductImage image =
                listPrimaryImage(
                        product,
                        "https://example.com/product.webp"
                );

        Pageable pageable =
                PageRequest.of(
                        0,
                        20
                );

        when(
                productRepository.findAll(
                        any(Specification.class),
                        eq(pageable)
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(product),
                        pageable,
                        1
                )
        );

        when(
                productImageRepository
                        .findAllByProduct_IdInAndPrimaryTrue(
                                List.of(1L)
                        )
        ).thenReturn(
                List.of(image)
        );

        PageResponse<ProductListItemResponse> response =
                productService.getProducts(
                        null,
                        null,
                        null,
                        null,
                        pageable
                );

        assertThat(response.items())
                .hasSize(1);

        ProductListItemResponse item =
                response.items().getFirst();

        assertThat(item.productId())
                .isEqualTo("1");

        assertThat(item.name())
                .isEqualTo("MCM Black Bag");

        assertThat(item.primaryImageUrl())
                .isEqualTo(
                        "https://example.com/product.webp"
                );

        assertThat(response.page())
                .isZero();

        assertThat(response.totalElements())
                .isEqualTo(1);
    }

    @Test
    void emptyProductPageDoesNotQueryImages() {
        Pageable pageable =
                PageRequest.of(
                        0,
                        20
                );

        when(
                productRepository.findAll(
                        any(Specification.class),
                        eq(pageable)
                )
        ).thenReturn(
                new PageImpl<>(
                        List.of(),
                        pageable,
                        0
                )
        );

        PageResponse<ProductListItemResponse> response =
                productService.getProducts(
                        null,
                        null,
                        null,
                        null,
                        pageable
                );

        assertThat(response.items())
                .isEmpty();

        verify(
                productImageRepository,
                never()
        ).findAllByProduct_IdInAndPrimaryTrue(
                any()
        );
    }

    @Test
    void productDetailGroupsImagesAndTags() {
        Product product =
                detailProduct(
                        1L,
                        "MCM-SKU-001",
                        "MCM Black Bag"
                );

        ProductImage image =
                detailImage(
                        "https://example.com/product.webp"
                );

        ProductTag style =
                productTag(
                        ProductTagType.STYLE,
                        "CASUAL"
                );

        ProductTag season =
                productTag(
                        ProductTagType.SEASON,
                        "ALL_SEASON"
                );

        ProductTag occasion =
                productTag(
                        ProductTagType.OCCASION,
                        "DAILY"
                );

        ProductTag feature =
                productTag(
                        ProductTagType.FEATURE,
                        "SPACIOUS"
                );
        ProductTagMapping styleMapping =
                mapping(style);

        ProductTagMapping seasonMapping =
                mapping(season);

        ProductTagMapping occasionMapping =
                mapping(occasion);

        ProductTagMapping featureMapping =
                mapping(feature);

        when(
                productRepository.findByIdAndStatus(
                        1L,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(
                Optional.of(product)
        );

        when(
                productImageRepository
                        .findAllByProduct_IdOrderBySortOrderAsc(
                                1L
                        )
        ).thenReturn(
                List.of(image)
        );

        when(
                productTagMappingRepository
                        .findAllWithTagByProductId(
                                1L
                        )
        ).thenReturn(
                List.of(
                        styleMapping,
                        seasonMapping,
                        occasionMapping,
                        featureMapping
                )
        );

        ProductDetailResponse response =
                productService.getProduct(1L);

        assertThat(response.productId())
                .isEqualTo("1");

        assertThat(response.images())
                .hasSize(1);

        assertThat(
                response.images()
                        .getFirst()
                        .isPrimary()
        ).isTrue();

        assertThat(response.tags().styles())
                .containsExactly("CASUAL");

        assertThat(response.tags().seasons())
                .containsExactly("ALL_SEASON");

        assertThat(response.tags().occasions())
                .containsExactly("DAILY");

        assertThat(response.tags().features())
                .containsExactly("SPACIOUS");
    }

    @Test
    void productWithoutTagsReturnsEmptyTagLists() {
        Product product =
                detailProduct(
                        1L,
                        "MCM-SKU-001",
                        "MCM Black Bag"
                );

        when(
                productRepository.findByIdAndStatus(
                        1L,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(
                Optional.of(product)
        );

        when(
                productImageRepository
                        .findAllByProduct_IdOrderBySortOrderAsc(
                                1L
                        )
        ).thenReturn(
                List.of()
        );

        when(
                productTagMappingRepository
                        .findAllWithTagByProductId(
                                1L
                        )
        ).thenReturn(
                List.of()
        );

        ProductDetailResponse response =
                productService.getProduct(1L);

        assertThat(response.images())
                .isEmpty();

        assertThat(response.tags().styles())
                .isEmpty();

        assertThat(response.tags().seasons())
                .isEmpty();

        assertThat(response.tags().occasions())
                .isEmpty();

        assertThat(response.tags().features())
                .isEmpty();
    }

    @Test
    void missingProductThrowsProductNotFound() {
        when(
                productRepository.findByIdAndStatus(
                        999L,
                        ProductStatus.ACTIVE
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(
                () ->
                        productService.getProduct(
                                999L
                        )
        )
                .isInstanceOf(
                        BusinessException.class
                )
                .satisfies(exception -> {
                    BusinessException businessException =
                            (BusinessException) exception;

                    assertThat(
                            businessException.getErrorCode()
                    ).isEqualTo(
                            ErrorCode.PRODUCT_NOT_FOUND
                    );
                });
    }

    private Product listProduct(
            Long id,
            String name
    ) {
        Product product =
                org.mockito.Mockito.mock(
                        Product.class
                );

        when(product.getId())
                .thenReturn(id);

        when(product.getBrand())
                .thenReturn(ProductBrand.MCM);

        when(product.getName())
                .thenReturn(name);

        when(product.getCategory())
                .thenReturn(ItemCategory.BAG);

        when(product.getPrice())
                .thenReturn(1_500_000L);

        when(product.getPrimaryColor())
                .thenReturn(ColorGroup.BLACK);

        return product;
    }

    private Product detailProduct(
            Long id,
            String sku,
            String name
    ) {
        Product product =
                org.mockito.Mockito.mock(
                        Product.class
                );

        when(product.getId())
                .thenReturn(id);

        when(product.getBrand())
                .thenReturn(ProductBrand.MCM);

        when(product.getSku())
                .thenReturn(sku);

        when(product.getName())
                .thenReturn(name);

        when(product.getCategory())
                .thenReturn(ItemCategory.BAG);

        when(product.getDescription())
                .thenReturn("제품 설명");

        when(product.getPrice())
                .thenReturn(1_500_000L);

        when(product.getPrimaryColor())
                .thenReturn(ColorGroup.BLACK);

        when(product.getMaterial())
                .thenReturn(MaterialGroup.LEATHER);

        when(product.getProductUrl())
                .thenReturn(
                        "https://example.com/product"
                );

        return product;
    }

    private ProductImage listPrimaryImage(
            Product product,
            String url
    ) {
        ProductImage image =
                org.mockito.Mockito.mock(
                        ProductImage.class
                );

        when(image.getProduct())
                .thenReturn(product);

        when(image.getUrl())
                .thenReturn(url);

        return image;
    }

    private ProductImage detailImage(
            String url
    ) {
        ProductImage image =
                org.mockito.Mockito.mock(
                        ProductImage.class
                );

        when(image.getUrl())
                .thenReturn(url);

        when(image.getAltText())
                .thenReturn("MCM Black Bag");

        when(image.getSortOrder())
                .thenReturn(0);

        when(image.isPrimary())
                .thenReturn(true);

        return image;
    }

    private ProductTag productTag(
            ProductTagType type,
            String code
    ) {
        ProductTag tag =
                org.mockito.Mockito.mock(
                        ProductTag.class
                );

        when(tag.getType())
                .thenReturn(type);

        when(tag.getCode())
                .thenReturn(code);

        return tag;
    }

    private ProductTagMapping mapping(
            ProductTag tag
    ) {
        ProductTagMapping mapping =
                org.mockito.Mockito.mock(
                        ProductTagMapping.class
                );

        when(mapping.getProductTag())
                .thenReturn(tag);

        return mapping;
    }
}