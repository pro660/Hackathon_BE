package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTag;
import org.likelionhsu.hackathon.product.entity.ProductTagMapping;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.likelionhsu.hackathon.preference.repository.PreferenceRepository;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanUserItemImageRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StylePlanRecommendationContextServiceTest {

    @Mock
    private PreferenceRepository preferenceRepository;
    @Mock
    private UserItemRepository userItemRepository;
    @Mock
    private StylePlanUserItemImageRepository
            userItemImageRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductTagMappingRepository
            productTagMappingRepository;
    @Mock
    private ProductImageRepository productImageRepository;

    private StylePlanRecommendationContextService service;

    @BeforeEach
    void setUp() {
        service = new StylePlanRecommendationContextService(
                preferenceRepository,
                userItemRepository,
                userItemImageRepository,
                productRepository,
                productTagMappingRepository,
                productImageRepository
        );
    }

    @Test
    void requestedStyleAndOccasionProductRanksFirst() {
        Product unmatched = product(
                101L,
                "기본 상품"
        );
        Product matched = product(
                102L,
                "데이트 상품"
        );

        ProductTagMapping neat =
                mapping(matched, "NEAT");
        ProductTagMapping date =
                mapping(matched, "DATE");

        when(preferenceRepository.findByUser_Id(1L))
                .thenReturn(Optional.empty());
        when(userItemRepository
                .findAllByUser_IdAndDeletedAtIsNullOrderByIdAsc(
                        1L
                )
        ).thenReturn(List.of());
        when(productRepository.findAllByBrand(
                ProductBrand.MCM
        )).thenReturn(
                List.of(unmatched, matched)
        );
        when(productTagMappingRepository
                .findAllWithTagByProductIdIn(
                        List.of(101L, 102L)
                )
        ).thenReturn(
                List.of(neat, date)
        );
        when(productImageRepository
                .findAllByProduct_IdInAndPrimaryTrue(
                        List.of(101L, 102L)
                )
        ).thenReturn(List.of());

        StylePlanRecommendationContext context =
                service.prepare(
                        1L,
                        new StylePlanJobRequest(
                                "DATE",
                                List.of("NEAT"),
                                null,
                                true,
                                "ko"
                        )
                );

        assertThat(context.productCandidates())
                .hasSize(2);
        assertThat(
                context.productCandidates()
                        .getFirst()
                        .productId()
        ).isEqualTo("102");
        assertThat(
                context.productCandidates()
                        .getFirst()
                        .score()
        ).isEqualTo(9);
        assertThat(
                context.productCandidates()
                        .get(1)
                        .score()
        ).isZero();
    }

    private Product product(
            Long id,
            String name
    ) {
        Product product = mock(Product.class);

        when(product.getId()).thenReturn(id);
        when(product.getName()).thenReturn(name);
        when(product.getStatus())
                .thenReturn(ProductStatus.ACTIVE);
        when(product.getCategory())
                .thenReturn(ItemCategory.BAG);

        return product;
    }

    private ProductTagMapping mapping(
            Product product,
            String code
    ) {
        ProductTag tag = mock(ProductTag.class);
        ProductTagMapping mapping =
                mock(ProductTagMapping.class);

        when(tag.getCode()).thenReturn(code);
        when(mapping.getProduct()).thenReturn(product);
        when(mapping.getProductTag()).thenReturn(tag);

        return mapping;
    }
}
