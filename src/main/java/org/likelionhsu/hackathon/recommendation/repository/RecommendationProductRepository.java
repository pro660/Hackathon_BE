package org.likelionhsu.hackathon.recommendation.repository;

import java.util.List;

import org.likelionhsu.hackathon.recommendation.entity.RecommendationProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationProductRepository
        extends JpaRepository<RecommendationProduct, Long> {

    @Query("""
            select recommendationProduct
            from RecommendationProduct recommendationProduct
            join fetch recommendationProduct.product
            where recommendationProduct.recommendation.id = :recommendationId
            order by recommendationProduct.rankOrder asc
            """)
    List<RecommendationProduct> findAllWithProductByRecommendationId(
            @Param("recommendationId") Long recommendationId
    );
}
