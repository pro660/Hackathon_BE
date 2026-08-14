package org.likelionhsu.hackathon.product.repository;

import java.util.List;

import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface ProductImageRepository
        extends JpaRepository<ProductImage, Long> {

    @Modifying
    @Query("""
        delete from ProductImage image
        where image.product.id = :productId
        """)
    int deleteAllByProductId(
            @Param("productId") Long productId
    );

    List<ProductImage> findAllByProduct_IdOrderBySortOrderAsc(
            Long productId
    );

    List<ProductImage> findAllByProduct_IdInAndPrimaryTrue(
            List<Long> productIds
    );
}