package org.likelionhsu.hackathon.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.cart.domain.CartItemData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
class CartItemRepositoryIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName(
                            "hackathon_cart_repository_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    private JdbcTemplate jdbcTemplate;
    private CartItemJdbcRepository repository;

    @BeforeEach
    void setUp() {
        Flyway.configure()
                .dataSource(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                )
                .locations("classpath:db/migration")
                .load()
                .migrate();

        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(
                        mysql.getJdbcUrl(),
                        mysql.getUsername(),
                        mysql.getPassword()
                );

        jdbcTemplate =
                new JdbcTemplate(dataSource);

        repository =
                new CartItemJdbcRepository(
                        jdbcTemplate
                );

        jdbcTemplate.update(
                "DELETE FROM cart_items"
        );
        jdbcTemplate.update(
                "DELETE FROM product_images"
        );
        jdbcTemplate.update(
                "DELETE FROM products"
        );
        jdbcTemplate.update(
                "DELETE FROM users"
        );
    }

    @Test
    void concurrentDuplicateAddsLeaveExactlyOneCartItem()
            throws Exception {
        TestIds ids =
                insertActiveProductFixture(
                        "cart-concurrency@example.com",
                        "MCM-CART-CONCURRENCY"
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        try {
            List<Future<Integer>> futures =
                    List.of(
                            executor.submit(
                                    () -> {
                                        ready.countDown();
                                        start.await();

                                        return repository
                                                .insertIfAbsent(
                                                        ids.userId(),
                                                        ids.productId()
                                                );
                                    }
                            ),
                            executor.submit(
                                    () -> {
                                        ready.countDown();
                                        start.await();

                                        return repository
                                                .insertIfAbsent(
                                                        ids.userId(),
                                                        ids.productId()
                                                );
                                    }
                            )
                    );

            assertThat(
                    ready.await(
                            5,
                            TimeUnit.SECONDS
                    )
            ).isTrue();

            start.countDown();

            for (Future<Integer> future : futures) {
                future.get(
                        10,
                        TimeUnit.SECONDS
                );
            }
        } finally {
            executor.shutdownNow();
        }

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM cart_items
                        WHERE user_id = ?
                          AND product_id = ?
                        """,
                        Integer.class,
                        ids.userId(),
                        ids.productId()
                );

        assertThat(count)
                .isEqualTo(1);
    }

    @Test
    void listUsesCurrentProductDataAndHidesInactiveProducts() {
        TestIds ids =
                insertActiveProductFixture(
                        "cart-list@example.com",
                        "MCM-CART-LIST"
                );

        jdbcTemplate.update(
                """
                INSERT INTO product_images (
                    product_id,
                    url,
                    public_id,
                    alt_text,
                    sort_order,
                    is_primary,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    ?,
                    NULL,
                    ?,
                    0,
                    TRUE,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                ids.productId(),
                "https://example.com/cart.webp",
                "Cart Product"
        );

        repository.insertIfAbsent(
                ids.userId(),
                ids.productId()
        );

        assertThat(
                repository.existsByUser_IdAndProduct_Id(
                        ids.userId(),
                        ids.productId()
                )
        ).isTrue();

        Page<CartItemData> page =
                repository.findAllActiveByUserId(
                        ids.userId(),
                        PageRequest.of(
                                0,
                                20,
                                Sort.by(
                                        Sort.Order.desc(
                                                "createdAt"
                                        )
                                )
                        )
                );

        assertThat(page.getTotalElements())
                .isEqualTo(1);

        CartItemData item =
                page.getContent().getFirst();

        assertThat(item.productId())
                .isEqualTo(ids.productId());
        assertThat(item.name())
                .isEqualTo("Current Cart Product");
        assertThat(item.price())
                .isEqualTo(1_000_000L);
        assertThat(item.primaryImageUrl())
                .isEqualTo(
                        "https://example.com/cart.webp"
                );
        assertThat(item.productUrl())
                .isEqualTo(
                        "https://example.com/product"
                );

        jdbcTemplate.update(
                """
                UPDATE products
                SET status = 'INACTIVE',
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """,
                ids.productId()
        );

        Page<CartItemData> hiddenPage =
                repository.findAllActiveByUserId(
                        ids.userId(),
                        PageRequest.of(
                                0,
                                20,
                                Sort.by(
                                        Sort.Order.desc(
                                                "createdAt"
                                        )
                                )
                        )
                );

        assertThat(hiddenPage.getContent())
                .isEmpty();

        assertThat(
                repository.existsByUser_IdAndProduct_Id(
                        ids.userId(),
                        ids.productId()
                )
        ).isTrue();

        repository.deleteByUser_IdAndProduct_Id(
                ids.userId(),
                ids.productId()
        );

        assertThat(
                repository.existsByUser_IdAndProduct_Id(
                        ids.userId(),
                        ids.productId()
                )
        ).isFalse();
    }

    private TestIds insertActiveProductFixture(
            String email,
            String sku
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    email,
                    nickname,
                    gender,
                    role,
                    status,
                    notification_email_verified,
                    version,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?,
                    'cart-user',
                    'NOT_SPECIFIED',
                    'USER',
                    'ACTIVE',
                    FALSE,
                    0,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """,
                email
        );

        Long userId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM users
                        WHERE email = ?
                        """,
                        Long.class,
                        email
                );

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
                    'Current Cart Product',
                    'BAG',
                    NULL,
                    1000000,
                    'BLACK',
                    'LEATHER',
                    'https://example.com/product',
                    'ACTIVE',
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """,
                sku
        );

        Long productId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM products
                        WHERE sku = ?
                        """,
                        Long.class,
                        sku
                );

        return new TestIds(
                userId,
                productId
        );
    }

    private record TestIds(
            Long userId,
            Long productId
    ) {
    }
}