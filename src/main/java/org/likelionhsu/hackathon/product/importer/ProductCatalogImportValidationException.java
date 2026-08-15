package org.likelionhsu.hackathon.product.importer;

public class ProductCatalogImportValidationException
        extends RuntimeException {

    public ProductCatalogImportValidationException(
            String message
    ) {
        super(message);
    }
}