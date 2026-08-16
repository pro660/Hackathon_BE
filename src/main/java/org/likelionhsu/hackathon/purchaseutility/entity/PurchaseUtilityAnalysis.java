package org.likelionhsu.hackathon.purchaseutility.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;

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
@Table(name = "purchase_utility_analyses")
public class PurchaseUtilityAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(
            name = "utility_score",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal utilityScore;

    @Column(name = "compatible_item_count", nullable = false)
    private int compatibleItemCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "factor_json", nullable = false)
    private PurchaseUtilityFactorSnapshot factorJson;

    @Column(name = "summary", length = 1500)
    private String summary;

    @Column(name = "ai_job_id")
    private Long aiJobId;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    protected PurchaseUtilityAnalysis() {
    }

    private PurchaseUtilityAnalysis(
            User user,
            Product product,
            BigDecimal utilityScore,
            int compatibleItemCount,
            PurchaseUtilityFactorSnapshot factorJson,
            String summary,
            Long aiJobId,
            Instant analyzedAt
    ) {
        this.user = user;
        this.product = product;
        this.utilityScore = utilityScore;
        this.compatibleItemCount = compatibleItemCount;
        this.factorJson = factorJson;
        this.summary = summary;
        this.aiJobId = aiJobId;
        this.analyzedAt = analyzedAt;
    }

    public static PurchaseUtilityAnalysis createRuleBased(
            User user,
            Product product,
            BigDecimal utilityScore,
            int compatibleItemCount,
            PurchaseUtilityFactorSnapshot factorJson,
            String summary,
            Long aiJobId,
            Instant analyzedAt
    ) {
        return new PurchaseUtilityAnalysis(
                user,
                product,
                utilityScore,
                compatibleItemCount,
                factorJson,
                summary,
                aiJobId,
                analyzedAt
        );
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getUtilityScore() {
        return utilityScore;
    }

    public int getCompatibleItemCount() {
        return compatibleItemCount;
    }

    public PurchaseUtilityFactorSnapshot getFactorJson() {
        return factorJson;
    }

    public String getSummary() {
        return summary;
    }

    public Long getAiJobId() {
        return aiJobId;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }
}
