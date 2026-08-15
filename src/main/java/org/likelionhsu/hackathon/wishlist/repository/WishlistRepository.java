package org.likelionhsu.hackathon.wishlist.repository;

import java.util.Collection;
import java.util.Set;

import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistRepository
        extends JpaRepository<Wishlist, Long> {

    boolean existsByUser_IdAndProduct_Id(
            Long userId,
            Long productId
    );

    void deleteByUser_IdAndProduct_Id(
            Long userId,
            Long productId
    );

    @EntityGraph(attributePaths = "product")
    Page<Wishlist> findAllByUser_IdAndProduct_Status(
            Long userId,
            ProductStatus status,
            Pageable pageable
    );

    @Query("""
            select w.product.id
            from Wishlist w
            where w.user.id = :userId
              and w.product.id in :productIds
            """)
    Set<Long> findProductIdsByUserIdAndProductIdIn(
            @Param("userId") Long userId,
            @Param("productIds") Collection<Long> productIds
    );
}