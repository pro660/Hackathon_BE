package org.likelionhsu.hackathon.recommendation.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationProductSnapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recommendation_products")
public class RecommendationProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "rank_order", nullable = false)
    private int rankOrder;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "reason", length = 1000)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_snapshot", nullable = false)
    private RecommendationProductSnapshot productSnapshot;

    protected RecommendationProduct() {
    }

    private RecommendationProduct(
            Recommendation recommendation,
            Product product,
            int rankOrder,
            BigDecimal score,
            String reason,
            RecommendationProductSnapshot productSnapshot
    ) {
        this.recommendation = recommendation;
        this.product = product;
        this.rankOrder = rankOrder;
        this.score = score;
        this.reason = reason;
        this.productSnapshot = productSnapshot;
    }

    public static RecommendationProduct create(
            Recommendation recommendation,
            Product product,
            int rankOrder,
            BigDecimal score,
            String reason,
            RecommendationProductSnapshot productSnapshot
    ) {
        return new RecommendationProduct(
                recommendation,
                product,
                rankOrder,
                score,
                reason,
                productSnapshot
        );
    }

    public Long getId() {
        return id;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    public Product getProduct() {
        return product;
    }

    public int getRankOrder() {
        return rankOrder;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }

    public RecommendationProductSnapshot getProductSnapshot() {
        return productSnapshot;
    }
}
