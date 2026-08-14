package org.likelionhsu.hackathon.product.importer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.importer.dto.ProductCatalogImportData;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportImage;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportItem;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportTag;

class ProductCatalogImportValidatorTest {

    private ProductCatalogImportValidator validator;

    @BeforeEach
    void setUp() {
        Validator beanValidator =
                Validation
                        .buildDefaultValidatorFactory()
                        .getValidator();

        validator =
                new ProductCatalogImportValidator(
                        beanValidator
                );
    }

    @Test
    void validSixtyProductCatalogPassesValidation() {
        ProductCatalogImportData data =
                validCatalog();

        assertThatCode(
                () -> validator.validate(data)
        ).doesNotThrowAnyException();
    }

    @Test
    void fiftyNineProductsFailValidation() {
        List<ProductImportItem> products =
                new ArrayList<>(
                        validProducts()
                );

        products.removeLast();

        ProductCatalogImportData data =
                new ProductCatalogImportData(
                        products
                );

        assertThatThrownBy(
                () -> validator.validate(data)
        )
                .isInstanceOf(
                        ProductCatalogImportValidationException.class
                )
                .hasMessageContaining(
                        "제품은 정확히 60개여야 합니다."
                );
    }

    @Test
    void duplicateSkuFailsValidation() {
        List<ProductImportItem> products =
                new ArrayList<>(
                        validProducts()
                );

        ProductImportItem first =
                products.getFirst();

        ProductImportItem last =
                products.getLast();

        products.set(
                products.size() - 1,
                copyWithSku(
                        last,
                        first.sku()
                )
        );

        ProductCatalogImportData data =
                new ProductCatalogImportData(
                        products
                );

        assertThatThrownBy(
                () -> validator.validate(data)
        )
                .isInstanceOf(
                        ProductCatalogImportValidationException.class
                )
                .hasMessageContaining(
                        "중복 SKU가 존재합니다."
                );
    }

    @Test
    void sectionCountMismatchFailsValidation() {
        List<ProductImportItem> products =
                new ArrayList<>(
                        validProducts()
                );

        ProductImportItem first =
                products.getFirst();

        products.set(
                0,
                copyWithSourceSection(
                        first,
                        ProductSourceSection.MEN
                )
        );

        ProductCatalogImportData data =
                new ProductCatalogImportData(
                        products
                );

        assertThatThrownBy(
                () -> validator.validate(data)
        )
                .isInstanceOf(
                        ProductCatalogImportValidationException.class
                )
                .hasMessageContaining(
                        "제품은 정확히 30개여야 합니다."
                );
    }

    @Test
    void sourceCategoryAndCategoryMismatchFailsValidation() {
        List<ProductImportItem> products =
                new ArrayList<>(
                        validProducts()
                );

        ProductImportItem first =
                products.getFirst();

        products.set(
                0,
                copyWithCategory(
                        first,
                        ItemCategory.SHOES
                )
        );

        ProductCatalogImportData data =
                new ProductCatalogImportData(
                        products
                );

        assertThatThrownBy(
                () -> validator.validate(data)
        )
                .isInstanceOf(
                        ProductCatalogImportValidationException.class
                )
                .hasMessageContaining(
                        "sourceCategory와 category가 일치하지 않습니다."
                );
    }

    @Test
    void nonPrimaryImageFailsValidation() {
        List<ProductImportItem> products =
                new ArrayList<>(
                        validProducts()
                );

        ProductImportItem first =
                products.getFirst();

        ProductImportImage invalidImage =
                new ProductImportImage(
                        "https://example.com/image.webp",
                        null,
                        first.name(),
                        0,
                        false
                );

        products.set(
                0,
                copyWithImages(
                        first,
                        List.of(invalidImage)
                )
        );

        ProductCatalogImportData data =
                new ProductCatalogImportData(
                        products
                );

        assertThatThrownBy(
                () -> validator.validate(data)
        )
                .isInstanceOf(
                        ProductCatalogImportValidationException.class
                )
                .hasMessageContaining(
                        "샘플 제품 이미지는 primary여야 합니다."
                );
    }

    @Test
    void missingOccasionTagFailsValidation() {
        List<ProductImportItem> products =
                new ArrayList<>(
                        validProducts()
                );

        ProductImportItem first =
                products.getFirst();

        List<ProductImportTag> tags =
                List.of(
                        new ProductImportTag(
                                ProductTagType.STYLE,
                                "CASUAL"
                        ),
                        new ProductImportTag(
                                ProductTagType.SEASON,
                                "ALL_SEASON"
                        )
                );

        products.set(
                0,
                copyWithTags(
                        first,
                        tags
                )
        );

        ProductCatalogImportData data =
                new ProductCatalogImportData(
                        products
                );

        assertThatThrownBy(
                () -> validator.validate(data)
        )
                .isInstanceOf(
                        ProductCatalogImportValidationException.class
                )
                .hasMessageContaining(
                        "OCCASION 태그가 최소 1개 필요합니다."
                );
    }

    @Test
    void blankSkuFailsBeanValidation() {
        List<ProductImportItem> products =
                new ArrayList<>(
                        validProducts()
                );

        ProductImportItem first =
                products.getFirst();

        products.set(
                0,
                copyWithSku(
                        first,
                        ""
                )
        );

        ProductCatalogImportData data =
                new ProductCatalogImportData(
                        products
                );

        assertThatThrownBy(
                () -> validator.validate(data)
        )
                .isInstanceOf(
                        ProductCatalogImportValidationException.class
                )
                .hasMessageContaining(
                        "Bean Validation 실패"
                )
                .hasMessageContaining(
                        "sku는 필수입니다."
                );
    }

    private ProductCatalogImportData validCatalog() {
        return new ProductCatalogImportData(
                validProducts()
        );
    }

    private List<ProductImportItem> validProducts() {
        List<ProductImportItem> products =
                new ArrayList<>();

        addCategoryProducts(
                products,
                ProductSourceSection.WOMEN,
                "핸드백",
                ItemCategory.BAG,
                "WOMEN-BAG"
        );

        addCategoryProducts(
                products,
                ProductSourceSection.WOMEN,
                "지갑 & 레더소품",
                ItemCategory.LEATHER_GOODS,
                "WOMEN-LEATHER"
        );

        addCategoryProducts(
                products,
                ProductSourceSection.WOMEN,
                "패션소품",
                ItemCategory.FASHION_ACCESSORY,
                "WOMEN-ACCESSORY"
        );

        addCategoryProducts(
                products,
                ProductSourceSection.WOMEN,
                "의류",
                ItemCategory.CLOTHING,
                "WOMEN-CLOTHING"
        );

        addCategoryProducts(
                products,
                ProductSourceSection.WOMEN,
                "슈즈",
                ItemCategory.SHOES,
                "WOMEN-SHOES"
        );

        addCategoryProducts(
                products,
                ProductSourceSection.MEN,
                "가방",
                ItemCategory.BAG,
                "MEN-BAG"
        );

        addCategoryProducts(
                products,
                ProductSourceSection.MEN,
                "지갑 & 레더소품",
                ItemCategory.LEATHER_GOODS,
                "MEN-LEATHER"
        );

        addCategoryProducts(
                products,
                ProductSourceSection.MEN,
                "패션소품",
                ItemCategory.FASHION_ACCESSORY,
                "MEN-ACCESSORY"
        );

        addCategoryProducts(
                products,
                ProductSourceSection.MEN,
                "의류",
                ItemCategory.CLOTHING,
                "MEN-CLOTHING"
        );

        addCategoryProducts(
                products,
                ProductSourceSection.MEN,
                "슈즈",
                ItemCategory.SHOES,
                "MEN-SHOES"
        );

        return products;
    }

    private void addCategoryProducts(
            List<ProductImportItem> products,
            ProductSourceSection section,
            String sourceCategory,
            ItemCategory category,
            String skuPrefix
    ) {
        for (int index = 1; index <= 6; index++) {

            String sku =
                    skuPrefix
                            + "-"
                            + index;

            String name =
                    "MCM Product "
                            + sku;

            products.add(
                    new ProductImportItem(
                            section,
                            sourceCategory,
                            sku,
                            ProductBrand.MCM,
                            name,
                            category,
                            "제품 설명",
                            1_000_000L + index,
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
                    )
            );
        }
    }

    private ProductImportItem copyWithSku(
            ProductImportItem source,
            String sku
    ) {
        return copy(
                source,
                source.sourceSection(),
                source.category(),
                sku,
                source.images(),
                source.tags()
        );
    }

    private ProductImportItem copyWithSourceSection(
            ProductImportItem source,
            ProductSourceSection section
    ) {
        return copy(
                source,
                section,
                source.category(),
                source.sku(),
                source.images(),
                source.tags()
        );
    }

    private ProductImportItem copyWithCategory(
            ProductImportItem source,
            ItemCategory category
    ) {
        return copy(
                source,
                source.sourceSection(),
                category,
                source.sku(),
                source.images(),
                source.tags()
        );
    }

    private ProductImportItem copyWithImages(
            ProductImportItem source,
            List<ProductImportImage> images
    ) {
        return copy(
                source,
                source.sourceSection(),
                source.category(),
                source.sku(),
                images,
                source.tags()
        );
    }

    private ProductImportItem copyWithTags(
            ProductImportItem source,
            List<ProductImportTag> tags
    ) {
        return copy(
                source,
                source.sourceSection(),
                source.category(),
                source.sku(),
                source.images(),
                tags
        );
    }

    private ProductImportItem copy(
            ProductImportItem source,
            ProductSourceSection section,
            ItemCategory category,
            String sku,
            List<ProductImportImage> images,
            List<ProductImportTag> tags
    ) {
        return new ProductImportItem(
                section,
                source.sourceCategory(),
                sku,
                source.brand(),
                source.name(),
                category,
                source.description(),
                source.price(),
                source.primaryColor(),
                source.material(),
                source.productUrl(),
                source.status(),
                images,
                tags
        );
    }
}