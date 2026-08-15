package org.likelionhsu.hackathon.product.importer;

public class ProductCatalogImportReadException
        extends RuntimeException {

    public ProductCatalogImportReadException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }
}