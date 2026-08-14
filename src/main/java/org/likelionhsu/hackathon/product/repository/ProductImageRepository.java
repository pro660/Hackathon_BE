package org.likelionhsu.hackathon.product.repository;

import java.util.List;

import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository
        extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findAllByProduct_IdOrderBySortOrderAsc(
            Long productId
    );

    List<ProductImage> findAllByProduct_IdInAndPrimaryTrue(
            List<Long> productIds
    );
}