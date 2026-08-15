package org.likelionhsu.hackathon.wishlist.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.config.ClockConfig;
import org.likelionhsu.hackathon.common.config.JpaAuditingConfig;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest(showSql = false)
@Import({
        ClockConfig.class,
        JpaAuditingConfig.class
})
class WishlistRepositoryTest {

    @Autowired
    WishlistRepository wishlistRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Test
    void wishlistCanBeSavedAndFound() {
        User user = saveUser(
                "wishlist@example.com",
                "찜사용자"
        );
        Product product = saveProduct(
                "MCM-WISHLIST-001",
                "Wishlist Bag"
        );

        wishlistRepository.saveAndFlush(
                Wishlist.create(user, product)
        );

        boolean exists =
                wishlistRepository
                        .existsByUser_IdAndProduct_Id(
                                user.getId(),
                                product.getId()
                        );

        assertThat(exists).isTrue();
    }

    @Test
    void duplicateUserAndProductCannotBeSaved() {
        User user = saveUser(
                "duplicate@example.com",
                "중복사용자"
        );
        Product product = saveProduct(
                "MCM-WISHLIST-002",
                "Duplicate Bag"
        );

        wishlistRepository.saveAndFlush(
                Wishlist.create(user, product)
        );

        assertThatThrownBy(
                () -> wishlistRepository.saveAndFlush(
                        Wishlist.create(user, product)
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void wishlistIsSeparatedByUser() {
        User firstUser = saveUser(
                "first@example.com",
                "첫사용자"
        );
        User secondUser = saveUser(
                "second@example.com",
                "둘사용자"
        );
        Product product = saveProduct(
                "MCM-WISHLIST-003",
                "Shared Bag"
        );

        wishlistRepository.saveAndFlush(
                Wishlist.create(
                        firstUser,
                        product
                )
        );

        assertThat(
                wishlistRepository
                        .existsByUser_IdAndProduct_Id(
                                firstUser.getId(),
                                product.getId()
                        )
        ).isTrue();

        assertThat(
                wishlistRepository
                        .existsByUser_IdAndProduct_Id(
                                secondUser.getId(),
                                product.getId()
                        )
        ).isFalse();
    }

    @Test
    void wishlistCanBeDeletedByUserAndProduct() {
        User user = saveUser(
                "delete@example.com",
                "삭제사용자"
        );
        Product product = saveProduct(
                "MCM-WISHLIST-004",
                "Delete Bag"
        );

        wishlistRepository.saveAndFlush(
                Wishlist.create(user, product)
        );

        wishlistRepository
                .deleteByUser_IdAndProduct_Id(
                        user.getId(),
                        product.getId()
                );

        wishlistRepository.flush();

        assertThat(
                wishlistRepository
                        .existsByUser_IdAndProduct_Id(
                                user.getId(),
                                product.getId()
                        )
        ).isFalse();
    }

    @Test
    void wishlistsCanBePagedByUser() {
        User user = saveUser(
                "page@example.com",
                "페이지사용자"
        );

        Product firstProduct = saveProduct(
                "MCM-WISHLIST-005",
                "First Bag"
        );
        Product secondProduct = saveProduct(
                "MCM-WISHLIST-006",
                "Second Bag"
        );

        wishlistRepository.saveAndFlush(
                Wishlist.create(
                        user,
                        firstProduct
                )
        );

        wishlistRepository.saveAndFlush(
                Wishlist.create(
                        user,
                        secondProduct
                )
        );

        var page =
                wishlistRepository.findAllByUser_Id(
                        user.getId(),
                        PageRequest.of(
                                0,
                                10,
                                Sort.by(
                                        Sort.Order.desc(
                                                "createdAt"
                                        )
                                )
                        )
                );

        assertThat(page.getTotalElements())
                .isEqualTo(2);

        assertThat(page.getContent())
                .extracting(
                        wishlist ->
                                wishlist.getProduct()
                                        .getId()
                )
                .containsExactlyInAnyOrder(
                        firstProduct.getId(),
                        secondProduct.getId()
                );
    }

    @Test
    void favoritedProductIdsCanBeFetchedInBatch() {
        User user = saveUser(
                "batch@example.com",
                "일괄사용자"
        );

        Product firstProduct = saveProduct(
                "MCM-WISHLIST-007",
                "First Favorite Bag"
        );
        Product secondProduct = saveProduct(
                "MCM-WISHLIST-008",
                "Second Favorite Bag"
        );
        Product notFavoritedProduct = saveProduct(
                "MCM-WISHLIST-009",
                "Not Favorite Bag"
        );

        wishlistRepository.saveAndFlush(
                Wishlist.create(
                        user,
                        firstProduct
                )
        );

        wishlistRepository.saveAndFlush(
                Wishlist.create(
                        user,
                        secondProduct
                )
        );

        Set<Long> favoritedProductIds =
                wishlistRepository
                        .findProductIdsByUserIdAndProductIdIn(
                                user.getId(),
                                List.of(
                                        firstProduct.getId(),
                                        secondProduct.getId(),
                                        notFavoritedProduct.getId()
                                )
                        );

        assertThat(favoritedProductIds)
                .containsExactlyInAnyOrder(
                        firstProduct.getId(),
                        secondProduct.getId()
                );
    }

    private User saveUser(
            String email,
            String nickname
    ) {
        return userRepository.saveAndFlush(
                User.local(
                        email,
                        nickname,
                        Gender.NOT_SPECIFIED
                )
        );
    }

    private Product saveProduct(
            String sku,
            String name
    ) {
        return productRepository.saveAndFlush(
                Product.create(
                        ProductBrand.MCM,
                        sku,
                        name,
                        ItemCategory.BAG,
                        null,
                        1_000_000L,
                        ColorGroup.BLACK,
                        MaterialGroup.LEATHER,
                        null,
                        ProductStatus.ACTIVE
                )
        );
    }
}