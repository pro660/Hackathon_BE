package org.likelionhsu.hackathon.product.importer.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ProductCatalogImportData(

        @NotEmpty(
                message = "products는 비어 있을 수 없습니다."
        )
        List<@Valid ProductImportItem> products
) {
}