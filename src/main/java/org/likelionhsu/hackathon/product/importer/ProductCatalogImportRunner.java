package org.likelionhsu.hackathon.product.importer;

import org.likelionhsu.hackathon.product.importer.dto.ProductCatalogImportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.product-import.enabled",
        havingValue = "true"
)
public class ProductCatalogImportRunner
        implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ProductCatalogImportRunner.class
            );

    private final ProductCatalogJsonReader jsonReader;
    private final ProductCatalogImporter importer;

    public ProductCatalogImportRunner(
            ProductCatalogJsonReader jsonReader,
            ProductCatalogImporter importer
    ) {
        this.jsonReader = jsonReader;
        this.importer = importer;
    }

    @Override
    public void run(
            ApplicationArguments args
    ) {
        ProductCatalogImportData data =
                jsonReader.read();

        importer.importCatalog(
                data
        );

        log.info(
                "Product catalog import completed. count={}",
                data.products().size()
        );
    }
}