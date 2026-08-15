package org.likelionhsu.hackathon.product.importer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.product.importer.dto.ProductCatalogImportData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest(
        properties = "app.product-import.enabled=false"
)
class ProductCatalogFinalResourceIntegrationTest {

    private static final int EXPECTED_PRODUCTS = 60;
    private static final int EXPECTED_IMAGES = 60;
    private static final int EXPECTED_TAG_MAPPINGS = 341;

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
    ProductCatalogJsonReader jsonReader;

    @Autowired
    ProductCatalogImportValidator validator;

    @Autowired
    ProductCatalogImporter importer;

    @Autowired
    JdbcTemplate jdbcTemplate;

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
    void finalCatalogPassesValidationAndImportsAll60Products() {
        ProductCatalogImportData data =
                jsonReader.read();

        assertThat(data.products())
                .hasSize(EXPECTED_PRODUCTS);

        // 실제 src/main/resources/data/mcm-products.json 전체를
        // Production Validator로 직접 검증한다.
        validator.validate(data);

        // 실제 Importer로 Testcontainers MySQL에 적재한다.
        importer.importCatalog(data);

        assertDatabaseState();

        // 동일한 JSON을 한 번 더 Import해도 중복이 생기지 않는지 확인한다.
        importer.importCatalog(data);

        assertDatabaseState();
    }

    private void assertDatabaseState() {
        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM products
                        WHERE brand = 'MCM'
                          AND status = 'ACTIVE'
                        """
                )
        ).isEqualTo(EXPECTED_PRODUCTS);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM product_images
                        """
                )
        ).isEqualTo(EXPECTED_IMAGES);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM product_tag_mappings
                        """
                )
        ).isEqualTo(EXPECTED_TAG_MAPPINGS);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM product_images
                        WHERE is_primary <> TRUE
                           OR sort_order <> 0
                        """
                )
        ).isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM product_images
                        WHERE url NOT LIKE 'https://res.cloudinary.com/%'
                        """
                )
        ).isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM product_images pi
                        JOIN products p
                          ON p.id = pi.product_id
                        WHERE pi.public_id
                              <> CONCAT(
                                     'hackathon/mcm-products/',
                                     p.sku,
                                     '-01'
                                 )
                        """
                )
        ).isZero();

        assertThat(
                countProductsMissingTagType("STYLE")
        ).isZero();

        assertThat(
                countProductsMissingTagType("SEASON")
        ).isZero();

        assertThat(
                countProductsMissingTagType("OCCASION")
        ).isZero();
    }

    private long countProductsMissingTagType(
            String type
    ) {
        Long result =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM products p
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM product_tag_mappings ptm
                            JOIN product_tags pt
                              ON pt.id = ptm.product_tag_id
                            WHERE ptm.product_id = p.id
                              AND pt.type = ?
                        )
                        """,
                        Long.class,
                        type
                );

        return result == null
                ? 0L
                : result;
    }

    private long count(
            String sql
    ) {
        Long result =
                jdbcTemplate.queryForObject(
                        sql,
                        Long.class
                );

        return result == null
                ? 0L
                : result;
    }
}
