package org.likelionhsu.hackathon.product.repository;

import java.util.Optional;

import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndStatus(
            Long id,
            ProductStatus status
    );

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);
}