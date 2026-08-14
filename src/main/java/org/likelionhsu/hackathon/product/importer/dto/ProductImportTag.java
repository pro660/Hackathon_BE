package org.likelionhsu.hackathon.product.importer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.likelionhsu.hackathon.product.entity.ProductTagType;

public record ProductImportTag(

        @NotNull(
                message = "tag type은 필수입니다."
        )
        ProductTagType type,

        @NotBlank(
                message = "tag code는 필수입니다."
        )
        @Size(
                max = 100,
                message = "tag code는 100자 이하여야 합니다."
        )
        String code
) {
}