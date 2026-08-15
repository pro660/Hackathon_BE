package org.likelionhsu.hackathon.product.repository;

import java.util.Optional;
import java.util.List;

import org.likelionhsu.hackathon.product.entity.ProductBrand;

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

    List<Product> findAllByBrand(
            ProductBrand brand
    );

    boolean existsBySku(String sku);
}