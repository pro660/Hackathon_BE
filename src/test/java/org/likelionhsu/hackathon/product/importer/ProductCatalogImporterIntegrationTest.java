package org.likelionhsu.hackathon.product.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.product.entity.Product;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.importer.dto.ProductCatalogImportData;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportImage;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportItem;
import org.likelionhsu.hackathon.product.importer.dto.ProductImportTag;

import org.likelionhsu.hackathon.wishlist.entity.Wishlist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(
        properties = "app.product-import.enabled=false"
)
@EntityScan(basePackageClasses = {
        Product.class,
        User.class,
        Wishlist.class
})
class ProductCatalogImporterIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                mysql::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                mysql::getUsername
        );

        registry.add(
                "spring.datasource.password",
                mysql::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    ProductCatalogImporter importer;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    ProductCatalogImportValidator catalogValidator;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "DELETE FROM product_tag_mappings"
        );

        jdbcTemplate.update(
                "DELETE FROM product_images"
        );

        jdbcTemplate.update(
                "DELETE FROM products"
        );
    }

    @Test
    void newProductAndRelationsArePersisted() {
        ProductCatalogImportData data =
                catalog(
                        product(
                                "MCM-IMPORT-001",
                                "Imported Bag",
                                "https://example.com/images/first.webp",
                                List.of(
                                        tag(
                                                ProductTagType.STYLE,
                                                "CASUAL"
                                        ),
                                        tag(
                                                ProductTagType.SEASON,
                                                "ALL_SEASON"
                                        ),
                                        tag(
                                                ProductTagType.OCCASION,
                                                "DAILY"
                                        )
                                )
                        )
                );

        importer.importCatalog(data);

        assertThat(
                countProducts(
                        "MCM-IMPORT-001"
                )
        ).isEqualTo(1L);

        assertThat(
                countImages(
                        "MCM-IMPORT-001"
                )
        ).isEqualTo(1L);

        assertThat(
                countTagMappings(
                        "MCM-IMPORT-001"
                )
        ).isEqualTo(3L);

        String imageUrl =
                jdbcTemplate.queryForObject(
                        """
                        SELECT pi.url
                        FROM product_images pi
                        JOIN products p
                          ON p.id = pi.product_id
                        WHERE p.sku = ?
                        """,
                        String.class,
                        "MCM-IMPORT-001"
                );

        assertThat(imageUrl)
                .isEqualTo(
                        "https://example.com/images/first.webp"
                );

        List<String> tags =
                findTags(
                        "MCM-IMPORT-001"
                );

        assertThat(tags)
                .containsExactlyInAnyOrder(
                        "STYLE:CASUAL",
                        "SEASON:ALL_SEASON",
                        "OCCASION:DAILY"
                );
    }

    @Test
    void reimportUpdatesProductWithoutCreatingDuplicate() {
        ProductCatalogImportData first =
                catalog(
                        product(
                                "MCM-IDEMPOTENT-001",
                                "Original Bag",
                                "https://example.com/images/original.webp",
                                List.of(
                                        tag(
                                                ProductTagType.STYLE,
                                                "CASUAL"
                                        ),
                                        tag(
                                                ProductTagType.SEASON,
                                                "ALL_SEASON"
                                        ),
                                        tag(
                                                ProductTagType.OCCASION,
                                                "DAILY"
                                        )
                                )
                        )
                );

        importer.importCatalog(first);

        ProductCatalogImportData second =
                catalog(
                        product(
                                "MCM-IDEMPOTENT-001",
                                "Updated Bag",
                                "https://example.com/images/updated.webp",
                                List.of(
                                        tag(
                                                ProductTagType.STYLE,
                                                "FORMAL"
                                        ),
                                        tag(
                                                ProductTagType.SEASON,
                                                "WINTER"
                                        ),
                                        tag(
                                                ProductTagType.OCCASION,
                                                "DATE"
                                        ),
                                        tag(
                                                ProductTagType.FEATURE,
                                                "COMPACT"
                                        )
                                )
                        )
                );

        importer.importCatalog(second);

        assertThat(
                countProducts(
                        "MCM-IDEMPOTENT-001"
                )
        ).isEqualTo(1L);

        String productName =
                jdbcTemplate.queryForObject(
                        """
                        SELECT name
                        FROM products
                        WHERE sku = ?
                        """,
                        String.class,
                        "MCM-IDEMPOTENT-001"
                );

        assertThat(productName)
                .isEqualTo("Updated Bag");

        assertThat(
                countImages(
                        "MCM-IDEMPOTENT-001"
                )
        ).isEqualTo(1L);

        String imageUrl =
                jdbcTemplate.queryForObject(
                        """
                        SELECT pi.url
                        FROM product_images pi
                        JOIN products p
                          ON p.id = pi.product_id
                        WHERE p.sku = ?
                        """,
                        String.class,
                        "MCM-IDEMPOTENT-001"
                );

        assertThat(imageUrl)
                .isEqualTo(
                        "https://example.com/images/updated.webp"
                );

        assertThat(
                findTags(
                        "MCM-IDEMPOTENT-001"
                )
        ).containsExactlyInAnyOrder(
                "STYLE:FORMAL",
                "SEASON:WINTER",
                "OCCASION:DATE",
                "FEATURE:COMPACT"
        );
    }

    @Test
    void missingMcmProductBecomesInactive() {
        insertExistingMcmProduct(
                "MCM-OLD-001"
        );

        ProductCatalogImportData data =
                catalog(
                        product(
                                "MCM-CURRENT-001",
                                "Current Bag",
                                "https://example.com/images/current.webp",
                                List.of(
                                        tag(
                                                ProductTagType.STYLE,
                                                "CASUAL"
                                        ),
                                        tag(
                                                ProductTagType.SEASON,
                                                "ALL_SEASON"
                                        ),
                                        tag(
                                                ProductTagType.OCCASION,
                                                "DAILY"
                                        )
                                )
                        )
                );

        importer.importCatalog(data);

        String status =
                jdbcTemplate.queryForObject(
                        """
                        SELECT status
                        FROM products
                        WHERE sku = ?
                        """,
                        String.class,
                        "MCM-OLD-001"
                );

        assertThat(status)
                .isEqualTo("INACTIVE");

        assertThat(
                countProducts(
                        "MCM-OLD-001"
                )
        ).isEqualTo(1L);
    }

    private ProductCatalogImportData catalog(
            ProductImportItem product
    ) {
        return new ProductCatalogImportData(
                List.of(product)
        );
    }

    private ProductImportItem product(
            String sku,
            String name,
            String imageUrl,
            List<ProductImportTag> tags
    ) {
        return new ProductImportItem(
                ProductSourceSection.WOMEN,
                "핸드백",
                sku,
                ProductBrand.MCM,
                name,
                ItemCategory.BAG,
                "제품 설명",
                1_500_000L,
                ColorGroup.BLACK,
                MaterialGroup.LEATHER,
                "https://example.com/products/"
                        + sku,
                ProductStatus.ACTIVE,
                List.of(
                        new ProductImportImage(
                                imageUrl,
                                null,
                                name,
                                0,
                                true
                        )
                ),
                tags
        );
    }

    private ProductImportTag tag(
            ProductTagType type,
            String code
    ) {
        return new ProductImportTag(
                type,
                code
        );
    }

    private long countProducts(
            String sku
    ) {
        Long count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM products
                        WHERE sku = ?
                        """,
                        Long.class,
                        sku
                );

        return count == null
                ? 0L
                : count;
    }

    private long countImages(
            String sku
    ) {
        Long count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM product_images pi
                        JOIN products p
                          ON p.id = pi.product_id
                        WHERE p.sku = ?
                        """,
                        Long.class,
                        sku
                );

        return count == null
                ? 0L
                : count;
    }

    private long countTagMappings(
            String sku
    ) {
        Long count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM product_tag_mappings ptm
                        JOIN products p
                          ON p.id = ptm.product_id
                        WHERE p.sku = ?
                        """,
                        Long.class,
                        sku
                );

        return count == null
                ? 0L
                : count;
    }

    private List<String> findTags(
            String sku
    ) {
        return jdbcTemplate.queryForList(
                """
                SELECT CONCAT(
                    pt.type,
                    ':',
                    pt.code
                )
                FROM product_tag_mappings ptm
                JOIN products p
                  ON p.id = ptm.product_id
                JOIN product_tags pt
                  ON pt.id = ptm.product_tag_id
                WHERE p.sku = ?
                """,
                String.class,
                sku
        );
    }

    private void insertExistingMcmProduct(
            String sku
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO products (
                    brand,
                    sku,
                    name,
                    category,
                    description,
                    price,
                    primary_color,
                    material,
                    product_url,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (
                    'MCM',
                    ?,
                    'Old MCM Product',
                    'BAG',
                    NULL,
                    1000000,
                    'BLACK',
                    'LEATHER',
                    NULL,
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                sku
        );
    }
}