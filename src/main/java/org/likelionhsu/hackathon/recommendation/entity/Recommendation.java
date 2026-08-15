package org.likelionhsu.hackathon.recommendation.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationContextSnapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recommendations")
public class Recommendation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false, length = 20)
    private RecommendationGenerationType generationType;

    @Column(name = "summary", length = 1000)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_json", nullable = false)
    private RecommendationContextSnapshot contextJson;

    @Column(name = "ai_job_id")
    private Long aiJobId;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected Recommendation() {
    }

    private Recommendation(
            User user,
            String summary,
            RecommendationContextSnapshot contextJson,
            Instant generatedAt
    ) {
        this.user = user;
        this.generationType = RecommendationGenerationType.RULE_BASED;
        this.summary = summary;
        this.contextJson = contextJson;
        this.aiJobId = null;
        this.generatedAt = generatedAt;
    }

    public static Recommendation createRuleBased(
            User user,
            String summary,
            RecommendationContextSnapshot contextJson,
            Instant generatedAt
    ) {
        return new Recommendation(
                user,
                summary,
                contextJson,
                generatedAt
        );
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public RecommendationGenerationType getGenerationType() {
        return generationType;
    }

    public String getSummary() {
        return summary;
    }

    public RecommendationContextSnapshot getContextJson() {
        return contextJson;
    }

    public Long getAiJobId() {
        return aiJobId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
