package org.likelionhsu.hackathon.product.importer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImportImage(

        @NotBlank(
                message = "이미지 url은 필수입니다."
        )
        @Size(
                max = 2048,
                message = "이미지 url은 2048자 이하여야 합니다."
        )
        String url,

        @Size(
                max = 255,
                message = "publicId는 255자 이하여야 합니다."
        )
        String publicId,

        @Size(
                max = 300,
                message = "altText는 300자 이하여야 합니다."
        )
        String altText,

        @Min(
                value = 0,
                message = "sortOrder는 0 이상이어야 합니다."
        )
        int sortOrder,

        boolean isPrimary
) {
}