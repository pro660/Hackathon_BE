package org.likelionhsu.hackathon.product.repository;

import java.util.List;
import java.util.Optional;

import org.likelionhsu.hackathon.product.entity.ProductTag;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTagRepository
        extends JpaRepository<ProductTag, Long> {

    Optional<ProductTag> findByTypeAndCode(
            ProductTagType type,
            String code
    );

    List<ProductTag> findAllByType(
            ProductTagType type
    );
}