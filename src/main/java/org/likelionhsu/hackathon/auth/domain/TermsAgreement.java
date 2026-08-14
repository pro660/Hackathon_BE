package org.likelionhsu.hackathon.auth.domain;

import java.time.Instant;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "terms_agreements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_terms_agreements_user_type_version",
                columnNames = {
                        "user_id",
                        "terms_type",
                        "terms_version"
                }
        )
)
public class TermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false, length = 40)
    private TermsType termsType;

    @Column(name = "terms_version", nullable = false, length = 30)
    private String termsVersion;

    @Column(nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at")
    private Instant agreedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    protected TermsAgreement() {
    }

    public TermsAgreement(
            User user,
            TermsType termsType,
            String termsVersion,
            boolean agreed,
            Instant decidedAt
    ) {
        this.user = user;
        this.termsType = termsType;
        this.termsVersion = termsVersion;
        this.agreed = agreed;
        this.agreedAt = agreed ? decidedAt : null;
        this.withdrawnAt = null;
    }

    public TermsType getTermsType() {
        return termsType;
    }

    public boolean isAgreed() {
        return agreed;
    }
}
