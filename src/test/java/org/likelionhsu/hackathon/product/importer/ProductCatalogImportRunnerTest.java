package org.likelionhsu.hackathon.product.importer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.product.importer.dto.ProductCatalogImportData;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class ProductCatalogImportRunnerTest {

    @Mock
    private ProductCatalogJsonReader jsonReader;

    @Mock
    private ProductCatalogImporter importer;

    private ProductCatalogImportRunner runner;

    @BeforeEach
    void setUp() {
        runner =
                new ProductCatalogImportRunner(
                        jsonReader,
                        importer
                );
    }

    @Test
    void runnerReadsCatalogAndImportsIt() {
        ProductCatalogImportData data =
                new ProductCatalogImportData(
                        List.of()
                );

        when(jsonReader.read())
                .thenReturn(data);

        ApplicationArguments arguments =
                mock(ApplicationArguments.class);

        runner.run(arguments);

        verify(jsonReader)
                .read();

        verify(importer)
                .importCatalog(data);
    }
}