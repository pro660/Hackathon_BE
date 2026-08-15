package org.likelionhsu.hackathon.product.importer.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.importer.ProductSourceSection;

public record ProductImportItem(

        @NotNull(
                message = "sourceSection은 필수입니다."
        )
        ProductSourceSection sourceSection,

        @NotBlank(
                message = "sourceCategory는 필수입니다."
        )
        @Size(
                max = 100,
                message = "sourceCategory는 100자 이하여야 합니다."
        )
        String sourceCategory,

        @NotBlank(
                message = "sku는 필수입니다."
        )
        @Size(
                max = 100,
                message = "sku는 100자 이하여야 합니다."
        )
        String sku,

        @NotNull(
                message = "brand는 필수입니다."
        )
        ProductBrand brand,

        @NotBlank(
                message = "name은 필수입니다."
        )
        @Size(
                max = 200,
                message = "name은 200자 이하여야 합니다."
        )
        String name,

        @NotNull(
                message = "category는 필수입니다."
        )
        ItemCategory category,

        @Size(
                max = 2000,
                message = "description은 2000자 이하여야 합니다."
        )
        String description,

        @NotNull(
                message = "price는 필수입니다."
        )
        @PositiveOrZero(
                message = "price는 0 이상이어야 합니다."
        )
        Long price,

        @NotNull(
                message = "primaryColor는 필수입니다."
        )
        ColorGroup primaryColor,

        MaterialGroup material,

        @NotBlank(
                message = "productUrl은 필수입니다."
        )
        @Size(
                max = 2048,
                message = "productUrl은 2048자 이하여야 합니다."
        )
        String productUrl,

        @NotNull(
                message = "status는 필수입니다."
        )
        ProductStatus status,

        @NotEmpty(
                message = "images는 비어 있을 수 없습니다."
        )
        List<@Valid ProductImportImage> images,

        @NotEmpty(
                message = "tags는 비어 있을 수 없습니다."
        )
        List<@Valid ProductImportTag> tags
) {
}