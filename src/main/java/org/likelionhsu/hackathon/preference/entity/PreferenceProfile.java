package org.likelionhsu.hackathon.preference.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "preference_profiles")
public class PreferenceProfile extends BaseTimeEntity {

    private static final String MANUAL_ANALYSIS_VERSION =
            "preference-manual-v1";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "preferred_colors",
            nullable = false
    )
    private List<ColorGroup> preferredColors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "preferred_categories",
            nullable = false
    )
    private List<ItemCategory> preferredCategories;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "preferred_style_tags",
            nullable = false
    )
    private List<PreferenceStyleTag> preferredStyleTags;

    @Column(
            name = "summary",
            length = 500
    )
    private String summary;

    @Column(
            name = "confidence",
            precision = 5,
            scale = 4
    )
    private BigDecimal confidence;

    @Column(
            name = "analysis_version",
            nullable = false,
            length = 50
    )
    private String analysisVersion;

    @Column(name = "ai_job_id")
    private Long aiJobId;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected PreferenceProfile() {
    }

    private PreferenceProfile(
            User user,
            List<ColorGroup> preferredColors,
            List<ItemCategory> preferredCategories,
            List<PreferenceStyleTag> preferredStyleTags
    ) {
        this.user = user;
        this.preferredColors =
                new ArrayList<>(preferredColors);
        this.preferredCategories =
                new ArrayList<>(preferredCategories);
        this.preferredStyleTags =
                new ArrayList<>(preferredStyleTags);
        resetAiState();
    }

    public static PreferenceProfile createManual(
            User user,
            List<ColorGroup> preferredColors,
            List<ItemCategory> preferredCategories,
            List<PreferenceStyleTag> preferredStyleTags
    ) {
        return new PreferenceProfile(
                user,
                preferredColors,
                preferredCategories,
                preferredStyleTags
        );
    }

    public boolean applyManualPreferences(
            List<ColorGroup> preferredColors,
            List<ItemCategory> preferredCategories,
            List<PreferenceStyleTag> preferredStyleTags
    ) {
        boolean samePreferences =
                this.preferredColors.equals(preferredColors)
                        && this.preferredCategories.equals(
                        preferredCategories
                )
                        && this.preferredStyleTags.equals(
                        preferredStyleTags
                );

        boolean alreadyManualState =
                this.summary == null
                        && this.confidence == null
                        && MANUAL_ANALYSIS_VERSION.equals(
                        this.analysisVersion
                )
                        && this.aiJobId == null
                        && this.analyzedAt == null;

        if (samePreferences && alreadyManualState) {
            return false;
        }

        this.preferredColors =
                new ArrayList<>(preferredColors);
        this.preferredCategories =
                new ArrayList<>(preferredCategories);
        this.preferredStyleTags =
                new ArrayList<>(preferredStyleTags);

        resetAiState();

        return true;
    }

    private void resetAiState() {
        this.summary = null;
        this.confidence = null;
        this.analysisVersion =
                MANUAL_ANALYSIS_VERSION;
        this.aiJobId = null;
        this.analyzedAt = null;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public List<ColorGroup> getPreferredColors() {
        return List.copyOf(preferredColors);
    }

    public List<ItemCategory> getPreferredCategories() {
        return List.copyOf(preferredCategories);
    }

    public List<PreferenceStyleTag> getPreferredStyleTags() {
        return List.copyOf(preferredStyleTags);
    }

    public String getSummary() {
        return summary;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getAnalysisVersion() {
        return analysisVersion;
    }

    public Long getAiJobId() {
        return aiJobId;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public Long getVersion() {
        return version;
    }
}