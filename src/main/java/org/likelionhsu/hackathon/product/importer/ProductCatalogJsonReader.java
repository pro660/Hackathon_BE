package org.likelionhsu.hackathon.product.importer;

import java.io.IOException;
import java.io.InputStream;

import org.likelionhsu.hackathon.product.importer.dto.ProductCatalogImportData;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProductCatalogJsonReader {

    private static final String CATALOG_RESOURCE =
            "data/mcm-products.json";

    private final JsonMapper jsonMapper;

    public ProductCatalogJsonReader(
            JsonMapper jsonMapper
    ) {
        this.jsonMapper = jsonMapper;
    }

    public ProductCatalogImportData read() {
        ClassPathResource resource =
                new ClassPathResource(
                        CATALOG_RESOURCE
                );

        try (
                InputStream inputStream =
                        resource.getInputStream()
        ) {
            return jsonMapper.readValue(
                    inputStream,
                    ProductCatalogImportData.class
            );
        } catch (
                IOException
                | JacksonException exception
        ) {
            throw new ProductCatalogImportReadException(
                    "제품 카탈로그 JSON을 읽을 수 없습니다.",
                    exception
            );
        }
    }
}