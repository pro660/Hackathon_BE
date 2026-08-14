package org.likelionhsu.hackathon.product.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
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
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTag;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.importer.dto.ProductCatalogImportData;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportImage;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportItem;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportTag;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductCatalogImporterTest {

    @Mock
    private ProductCatalogImportValidator catalogValidator;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductTagRepository productTagRepository;

    @Mock
    private ProductTagMappingRepository productTagMappingRepository;

    private ProductCatalogImporter importer;

    @BeforeEach
    void setUp() {
        importer =
                new ProductCatalogImporter(
                        catalogValidator,
                        productRepository,
                        productImageRepository,
                        productTagRepository,
                        productTagMappingRepository
                );
    }

    @Test
    void newProductIsCreated() {
        ProductCatalogImportData data =
                catalog(
                        productItem(
                                "MCM-NEW-001",
                                "New MCM Bag"
                        )
                );

        when(productRepository.findAllByBrand(ProductBrand.MCM))
                .thenReturn(List.of());

        when(productRepository.findBySku("MCM-NEW-001"))
                .thenReturn(Optional.empty());

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        List<ProductTag> tags =
                standardTags();

        when(productTagRepository.findAll())
                .thenReturn(tags);

        importer.importCatalog(data);

        ArgumentCaptor<Product> productCaptor =
                ArgumentCaptor.forClass(
                        Product.class
                );

        verify(productRepository)
                .save(productCaptor.capture());

        Product savedProduct =
                productCaptor.getValue();

        assertThat(savedProduct.getSku())
                .isEqualTo("MCM-NEW-001");

        assertThat(savedProduct.getName())
                .isEqualTo("New MCM Bag");

        assertThat(savedProduct.getBrand())
                .isEqualTo(ProductBrand.MCM);

        assertThat(savedProduct.getCategory())
                .isEqualTo(ItemCategory.BAG);

        assertThat(savedProduct.getStatus())
                .isEqualTo(ProductStatus.ACTIVE);

        verify(productImageRepository)
                .saveAll(anyList());

        verify(productTagMappingRepository)
                .saveAll(anyList());

        verify(catalogValidator)
                .validate(data);
    }

    @Test
    void existingProductIsUpdatedInsteadOfCreated() {
        Product existing =
                Product.create(
                        ProductBrand.MCM,
                        "MCM-EXISTING-001",
                        "Old Name",
                        ItemCategory.BAG,
                        "Old description",
                        500_000L,
                        ColorGroup.BROWN,
                        MaterialGroup.LEATHER,
                        "https://example.com/old",
                        ProductStatus.ACTIVE
                );

        ProductCatalogImportData data =
                catalog(
                        productItem(
                                "MCM-EXISTING-001",
                                "Updated MCM Bag"
                        )
                );

        when(productRepository.findAllByBrand(ProductBrand.MCM))
                .thenReturn(
                        List.of(existing)
                );

        when(productRepository.findBySku("MCM-EXISTING-001"))
                .thenReturn(
                        Optional.of(existing)
                );

        List<ProductTag> tags =
                standardTags();

        when(productTagRepository.findAll())
                .thenReturn(tags);

        importer.importCatalog(data);

        verify(
                productRepository,
                never()
        ).save(any(Product.class));

        assertThat(existing.getName())
                .isEqualTo("Updated MCM Bag");

        assertThat(existing.getPrice())
                .isEqualTo(1_500_000L);

        assertThat(existing.getPrimaryColor())
                .isEqualTo(ColorGroup.BLACK);

        assertThat(existing.getProductUrl())
                .isEqualTo(
                        "https://example.com/products/MCM-EXISTING-001"
                );

        verify(productImageRepository)
                .deleteAllByProductId(
                        existing.getId()
                );

        verify(productImageRepository)
                .saveAll(anyList());

        verify(productTagMappingRepository)
                .deleteAllByProductId(
                        existing.getId()
                );

        verify(productTagMappingRepository)
                .saveAll(anyList());
    }

    @Test
    void productMissingFromImportedCatalogBecomesInactive() {
        Product missingProduct =
                Product.create(
                        ProductBrand.MCM,
                        "MCM-OLD-001",
                        "Old MCM Product",
                        ItemCategory.BAG,
                        null,
                        900_000L,
                        ColorGroup.BLACK,
                        MaterialGroup.LEATHER,
                        "https://example.com/old-product",
                        ProductStatus.ACTIVE
                );

        ProductCatalogImportData data =
                catalog(
                        productItem(
                                "MCM-CURRENT-001",
                                "Current MCM Bag"
                        )
                );

        when(productRepository.findAllByBrand(ProductBrand.MCM))
                .thenReturn(
                        List.of(missingProduct)
                );

        when(productRepository.findBySku("MCM-CURRENT-001"))
                .thenReturn(Optional.empty());

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        List<ProductTag> tags =
                standardTags();

        when(productTagRepository.findAll())
                .thenReturn(tags);

        importer.importCatalog(data);

        assertThat(missingProduct.getStatus())
                .isEqualTo(
                        ProductStatus.INACTIVE
                );
    }

    @Test
    void imagesAndTagMappingsAreReplaced() {
        Product existing =
                Product.create(
                        ProductBrand.MCM,
                        "MCM-REPLACE-001",
                        "Existing Bag",
                        ItemCategory.BAG,
                        null,
                        1_000_000L,
                        ColorGroup.BLACK,
                        MaterialGroup.LEATHER,
                        "https://example.com/existing",
                        ProductStatus.ACTIVE
                );

        ProductCatalogImportData data =
                catalog(
                        productItem(
                                "MCM-REPLACE-001",
                                "Replacement Bag"
                        )
                );

        when(productRepository.findAllByBrand(ProductBrand.MCM))
                .thenReturn(
                        List.of(existing)
                );

        when(productRepository.findBySku("MCM-REPLACE-001"))
                .thenReturn(
                        Optional.of(existing)
                );

        List<ProductTag> tags =
                standardTags();

        when(productTagRepository.findAll())
                .thenReturn(tags);

        importer.importCatalog(data);

        verify(productImageRepository)
                .deleteAllByProductId(
                        existing.getId()
                );

        verify(productImageRepository)
                .saveAll(anyList());

        verify(productTagMappingRepository)
                .deleteAllByProductId(
                        existing.getId()
                );

        verify(productTagMappingRepository)
                .saveAll(anyList());
    }

    @Test
    void unknownProductTagFailsImport() {
        ProductCatalogImportData data =
                catalog(
                        productItem(
                                "MCM-UNKNOWN-TAG-001",
                                "Unknown Tag Bag"
                        )
                );

        when(productRepository.findAllByBrand(ProductBrand.MCM))
                .thenReturn(List.of());

        when(productRepository.findBySku("MCM-UNKNOWN-TAG-001"))
                .thenReturn(Optional.empty());

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(productTagRepository.findAll())
                .thenReturn(
                        List.of()
                );

        assertThatThrownBy(
                () -> importer.importCatalog(data)
        )
                .isInstanceOf(
                        ProductCatalogImportValidationException.class
                )
                .hasMessageContaining(
                        "DB에 존재하지 않는 ProductTag입니다."
                );

        verify(
                productTagMappingRepository,
                never()
        ).saveAll(anyList());
    }

    @Test
    void duplicateImportedTagFailsImport() {
        ProductImportItem source =
                productItem(
                        "MCM-DUPLICATE-TAG-001",
                        "Duplicate Tag Bag"
                );

        ProductImportItem duplicateTagProduct =
                new ProductImportItem(
                        source.sourceSection(),
                        source.sourceCategory(),
                        source.sku(),
                        source.brand(),
                        source.name(),
                        source.category(),
                        source.description(),
                        source.price(),
                        source.primaryColor(),
                        source.material(),
                        source.productUrl(),
                        source.status(),
                        source.images(),
                        List.of(
                                new ProductImportTag(
                                        ProductTagType.STYLE,
                                        "CASUAL"
                                ),
                                new ProductImportTag(
                                        ProductTagType.STYLE,
                                        "CASUAL"
                                ),
                                new ProductImportTag(
                                        ProductTagType.SEASON,
                                        "ALL_SEASON"
                                ),
                                new ProductImportTag(
                                        ProductTagType.OCCASION,
                                        "DAILY"
                                )
                        )
                );

        ProductCatalogImportData data =
                catalog(
                        duplicateTagProduct
                );

        when(productRepository.findAllByBrand(ProductBrand.MCM))
                .thenReturn(List.of());

        when(productRepository.findBySku("MCM-DUPLICATE-TAG-001"))
                .thenReturn(Optional.empty());

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        List<ProductTag> tags =
                standardTags();

        when(productTagRepository.findAll())
                .thenReturn(tags);

        assertThatThrownBy(
                () -> importer.importCatalog(data)
        )
                .isInstanceOf(
                        ProductCatalogImportValidationException.class
                )
                .hasMessageContaining(
                        "중복 태그가 존재합니다."
                );

        verify(
                productTagMappingRepository,
                never()
        ).saveAll(anyList());
    }

    private ProductCatalogImportData catalog(
            ProductImportItem product
    ) {
        return new ProductCatalogImportData(
                List.of(product)
        );
    }

    private ProductImportItem productItem(
            String sku,
            String name
    ) {
        return new ProductImportItem(
                ProductSourceSection.WOMEN,
                "핸드백",
                sku,
                ProductBrand.MCM,
                name,
                ItemCategory.BAG,
                "제품 설명",
                1_500_000L,
                ColorGroup.BLACK,
                MaterialGroup.LEATHER,
                "https://example.com/products/"
                        + sku,
                ProductStatus.ACTIVE,
                List.of(
                        new ProductImportImage(
                                "https://example.com/images/"
                                        + sku
                                        + ".webp",
                                null,
                                name,
                                0,
                                true
                        )
                ),
                List.of(
                        new ProductImportTag(
                                ProductTagType.STYLE,
                                "CASUAL"
                        ),
                        new ProductImportTag(
                                ProductTagType.SEASON,
                                "ALL_SEASON"
                        ),
                        new ProductImportTag(
                                ProductTagType.OCCASION,
                                "DAILY"
                        )
                )
        );
    }

    private List<ProductTag> standardTags() {
        return List.of(
                productTag(
                        ProductTagType.STYLE,
                        "CASUAL"
                ),
                productTag(
                        ProductTagType.SEASON,
                        "ALL_SEASON"
                ),
                productTag(
                        ProductTagType.OCCASION,
                        "DAILY"
                )
        );
    }

    private ProductTag productTag(
            ProductTagType type,
            String code
    ) {
        ProductTag tag =
                mock(ProductTag.class);

        when(tag.getType())
                .thenReturn(type);

        when(tag.getCode())
                .thenReturn(code);

        return tag;
    }
}