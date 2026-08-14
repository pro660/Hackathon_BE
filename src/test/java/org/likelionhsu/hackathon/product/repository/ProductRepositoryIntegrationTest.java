package org.likelionhsu.hackathon.product.repository;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@EntityScan(basePackages = "org.likelionhsu.hackathon.product.entity")
class ProductRepositoryIntegrationTest {

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
    ProductRepository productRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "DELETE FROM products"
        );

        insertProduct(
                "MCM-BAG-001",
                "Black MCM Bag",
                "BAG",
                1_500_000L,
                "BLACK",
                "ACTIVE"
        );

        insertProduct(
                "MCM-BAG-002",
                "Brown MCM Bag",
                "BAG",
                2_500_000L,
                "BROWN",
                "ACTIVE"
        );

        insertProduct(
                "MCM-SHOES-001",
                "Black MCM Shoes",
                "SHOES",
                900_000L,
                "BLACK",
                "ACTIVE"
        );

        insertProduct(
                "MCM-INACTIVE-001",
                "Inactive MCM Bag",
                "BAG",
                1_200_000L,
                "BLACK",
                "INACTIVE"
        );
    }

    @Test
    void activeProductCanBeFoundBySku() {
        Product product =
                productRepository
                        .findBySku("MCM-BAG-001")
                        .orElseThrow();

        assertThat(product.getName())
                .isEqualTo("Black MCM Bag");

        assertThat(product.getCategory())
                .isEqualTo(ItemCategory.BAG);

        assertThat(product.getPrimaryColor())
                .isEqualTo(ColorGroup.BLACK);

        assertThat(product.getStatus())
                .isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void filtersProductsByCategoryColorAndPriceRange() {
        Specification<Product> specification =
                ProductSpecification
                        .hasStatus(ProductStatus.ACTIVE)
                        .and(
                                ProductSpecification.hasCategory(
                                        ItemCategory.BAG
                                )
                        )
                        .and(
                                ProductSpecification.hasPrimaryColor(
                                        ColorGroup.BLACK
                                )
                        )
                        .and(
                                ProductSpecification
                                        .priceGreaterThanOrEqualTo(
                                                1_000_000L
                                        )
                        )
                        .and(
                                ProductSpecification
                                        .priceLessThanOrEqualTo(
                                                2_000_000L
                                        )
                        );

        List<Product> products =
                productRepository.findAll(specification);

        assertThat(products)
                .hasSize(1);

        assertThat(products.getFirst().getSku())
                .isEqualTo("MCM-BAG-001");
    }

    @Test
    void inactiveProductIsExcludedWhenActiveStatusIsApplied() {
        Specification<Product> specification =
                ProductSpecification
                        .hasStatus(ProductStatus.ACTIVE)
                        .and(
                                ProductSpecification.hasCategory(
                                        ItemCategory.BAG
                                )
                        );

        List<Product> products =
                productRepository.findAll(specification);

        assertThat(products)
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder(
                        "MCM-BAG-001",
                        "MCM-BAG-002"
                );
    }

    @Test
    void productCanBeFoundByIdAndActiveStatus() {
        Product product =
                productRepository
                        .findBySku("MCM-BAG-001")
                        .orElseThrow();

        assertThat(
                productRepository.findByIdAndStatus(
                        product.getId(),
                        ProductStatus.ACTIVE
                )
        ).isPresent();

        assertThat(
                productRepository.findByIdAndStatus(
                        product.getId(),
                        ProductStatus.INACTIVE
                )
        ).isEmpty();
    }

    private void insertProduct(
            String sku,
            String name,
            String category,
            long price,
            String primaryColor,
            String status
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
                    ?,
                    ?,
                    NULL,
                    ?,
                    ?,
                    'LEATHER',
                    NULL,
                    ?,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                sku,
                name,
                category,
                price,
                primaryColor,
                status
        );
    }
}