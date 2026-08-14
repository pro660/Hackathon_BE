package org.likelionhsu.hackathon.product.repository;

import java.util.List;

import org.likelionhsu.hackathon.product.entity.ProductTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

public interface ProductTagMappingRepository
        extends JpaRepository<ProductTagMapping, Long> {

    @Query("""
            select mapping
            from ProductTagMapping mapping
            join fetch mapping.productTag
            where mapping.product.id = :productId
            """)
    List<ProductTagMapping> findAllWithTagByProductId(
            @Param("productId") Long productId
    );

    @Modifying
    @Query("""
        delete from ProductTagMapping mapping
        where mapping.product.id = :productId
        """)
    int deleteAllByProductId(
            @Param("productId") Long productId
    );
}