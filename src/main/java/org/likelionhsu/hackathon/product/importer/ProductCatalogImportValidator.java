package org.likelionhsu.hackathon.product.importer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.importer.dto.ProductCatalogImportData;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportItem;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportTag;
import org.springframework.stereotype.Component;

@Component
public class ProductCatalogImportValidator {

    private static final int EXPECTED_TOTAL_COUNT = 60;
    private static final int EXPECTED_SECTION_COUNT = 30;
    private static final int EXPECTED_SOURCE_CATEGORY_COUNT = 6;

    private static final Map<ProductSourceSection, Map<String, ItemCategory>>
            EXPECTED_SOURCE_CATEGORIES =
            Map.of(
                    ProductSourceSection.WOMEN,
                    Map.of(
                            "핸드백",
                            ItemCategory.BAG,
                            "지갑 & 레더소품",
                            ItemCategory.LEATHER_GOODS,
                            "패션소품",
                            ItemCategory.FASHION_ACCESSORY,
                            "의류",
                            ItemCategory.CLOTHING,
                            "슈즈",
                            ItemCategory.SHOES
                    ),
                    ProductSourceSection.MEN,
                    Map.of(
                            "가방",
                            ItemCategory.BAG,
                            "지갑 & 레더소품",
                            ItemCategory.LEATHER_GOODS,
                            "패션소품",
                            ItemCategory.FASHION_ACCESSORY,
                            "의류",
                            ItemCategory.CLOTHING,
                            "슈즈",
                            ItemCategory.SHOES
                    )
            );

    private final Validator validator;

    public ProductCatalogImportValidator(
            Validator validator
    ) {
        this.validator = validator;
    }

    public void validate(
            ProductCatalogImportData data
    ) {
        validateBeanConstraints(data);

        List<ProductImportItem> products =
                data.products();

        validateTotalCount(products);
        validateSectionCounts(products);
        validateSourceCategories(products);
        validateUniqueSkus(products);
        validateBrandAndStatus(products);
        validateImages(products);
        validateRequiredTagTypes(products);
    }

    private void validateBeanConstraints(
            ProductCatalogImportData data
    ) {
        Set<ConstraintViolation<ProductCatalogImportData>> violations =
                validator.validate(data);

        if (violations.isEmpty()) {
            return;
        }

        String message =
                violations.stream()
                        .map(violation ->
                                violation.getPropertyPath()
                                        + ": "
                                        + violation.getMessage()
                        )
                        .sorted()
                        .collect(
                                Collectors.joining(", ")
                        );

        fail(
                "Bean Validation 실패: "
                        + message
        );
    }

    private void validateTotalCount(
            List<ProductImportItem> products
    ) {
        if (products.size() != EXPECTED_TOTAL_COUNT) {
            fail(
                    "제품은 정확히 60개여야 합니다. actual="
                            + products.size()
            );
        }
    }

    private void validateSectionCounts(
            List<ProductImportItem> products
    ) {
        for (ProductSourceSection section
                : ProductSourceSection.values()) {

            long count =
                    products.stream()
                            .filter(product ->
                                    product.sourceSection()
                                            == section
                            )
                            .count();

            if (count != EXPECTED_SECTION_COUNT) {
                fail(
                        section
                                + " 제품은 정확히 30개여야 합니다. actual="
                                + count
                );
            }
        }
    }

    private void validateSourceCategories(
            List<ProductImportItem> products
    ) {
        for (ProductImportItem product : products) {

            Map<String, ItemCategory> expectedCategories =
                    EXPECTED_SOURCE_CATEGORIES.get(
                            product.sourceSection()
                    );

            ItemCategory expectedCategory =
                    expectedCategories.get(
                            product.sourceCategory()
                    );

            if (expectedCategory == null) {
                fail(
                        "지원하지 않는 sourceCategory입니다. "
                                + "section="
                                + product.sourceSection()
                                + ", sourceCategory="
                                + product.sourceCategory()
                );
            }

            if (product.category() != expectedCategory) {
                fail(
                        "sourceCategory와 category가 일치하지 않습니다. "
                                + "sku="
                                + product.sku()
                );
            }
        }

        for (var sectionEntry
                : EXPECTED_SOURCE_CATEGORIES.entrySet()) {

            ProductSourceSection section =
                    sectionEntry.getKey();

            for (String sourceCategory
                    : sectionEntry.getValue().keySet()) {

                long count =
                        products.stream()
                                .filter(product ->
                                        product.sourceSection()
                                                == section
                                )
                                .filter(product ->
                                        product.sourceCategory()
                                                .equals(
                                                        sourceCategory
                                                )
                                )
                                .count();

                if (count
                        != EXPECTED_SOURCE_CATEGORY_COUNT) {

                    fail(
                            section
                                    + " / "
                                    + sourceCategory
                                    + " 제품은 정확히 6개여야 합니다. actual="
                                    + count
                    );
                }
            }
        }
    }

    private void validateUniqueSkus(
            List<ProductImportItem> products
    ) {
        Set<String> skus =
                new HashSet<>();

        for (ProductImportItem product : products) {
            if (!skus.add(product.sku())) {
                fail(
                        "중복 SKU가 존재합니다. sku="
                                + product.sku()
                );
            }
        }
    }

    private void validateBrandAndStatus(
            List<ProductImportItem> products
    ) {
        for (ProductImportItem product : products) {

            if (product.brand() != ProductBrand.MCM) {
                fail(
                        "샘플 카탈로그 brand는 MCM이어야 합니다. sku="
                                + product.sku()
                );
            }

            if (product.status() != ProductStatus.ACTIVE) {
                fail(
                        "샘플 카탈로그 status는 ACTIVE여야 합니다. sku="
                                + product.sku()
                );
            }
        }
    }

    private void validateImages(
            List<ProductImportItem> products
    ) {
        for (ProductImportItem product : products) {

            if (product.images().size() != 1) {
                fail(
                        "MVP 샘플 제품은 이미지가 정확히 1개여야 합니다. sku="
                                + product.sku()
                );
            }

            var image =
                    product.images().getFirst();

            if (!image.isPrimary()) {
                fail(
                        "샘플 제품 이미지는 primary여야 합니다. sku="
                                + product.sku()
                );
            }

            if (image.sortOrder() != 0) {
                fail(
                        "샘플 제품 이미지 sortOrder는 0이어야 합니다. sku="
                                + product.sku()
                );
            }
        }
    }

    private void validateRequiredTagTypes(
            List<ProductImportItem> products
    ) {
        for (ProductImportItem product : products) {

            Set<ProductTagType> tagTypes =
                    product.tags()
                            .stream()
                            .map(ProductImportTag::type)
                            .collect(
                                    Collectors.toSet()
                            );

            requireTagType(
                    product,
                    tagTypes,
                    ProductTagType.STYLE
            );

            requireTagType(
                    product,
                    tagTypes,
                    ProductTagType.SEASON
            );

            requireTagType(
                    product,
                    tagTypes,
                    ProductTagType.OCCASION
            );
        }
    }

    private void requireTagType(
            ProductImportItem product,
            Set<ProductTagType> tagTypes,
            ProductTagType requiredType
    ) {
        if (!tagTypes.contains(requiredType)) {
            fail(
                    requiredType
                            + " 태그가 최소 1개 필요합니다. sku="
                            + product.sku()
            );
        }
    }

    private void fail(
            String message
    ) {
        throw new ProductCatalogImportValidationException(
                message
        );
    }
}