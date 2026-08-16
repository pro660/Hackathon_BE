package org.likelionhsu.hackathon.auth.domain;

import java.time.Instant;
import java.util.Locale;

import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "pending_social_signups",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pending_social_signups_provider_user",
                        columnNames = {
                                "provider",
                                "provider_user_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_pending_social_signups_token_hash",
                        columnNames = "onboarding_token_hash"
                )
        }
)
public class PendingSocialSignup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 320)
    private String providerEmail;

    @Column(
            name = "onboarding_token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String onboardingTokenHash;

    @Column(name = "onboarding_token_expires_at", nullable = false)
    private Instant onboardingTokenExpiresAt;

    @Column(name = "onboarding_token_consumed_at")
    private Instant onboardingTokenConsumedAt;

    protected PendingSocialSignup() {
    }

    public PendingSocialSignup(
            SocialProvider provider,
            String providerUserId,
            String providerEmail,
            String onboardingTokenHash,
            Instant onboardingTokenExpiresAt
    ) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        renew(
                providerEmail,
                onboardingTokenHash,
                onboardingTokenExpiresAt
        );
    }

    public void renew(
            String providerEmail,
            String onboardingTokenHash,
            Instant onboardingTokenExpiresAt
    ) {
        this.providerEmail = normalizeEmail(providerEmail);
        this.onboardingTokenHash = onboardingTokenHash;
        this.onboardingTokenExpiresAt = onboardingTokenExpiresAt;
        this.onboardingTokenConsumedAt = null;
    }

    public boolean isUsableAt(Instant now) {
        return onboardingTokenConsumedAt == null
                && onboardingTokenExpiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        onboardingTokenConsumedAt = now;
    }

    public Long getId() {
        return id;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public String getOnboardingTokenHash() {
        return onboardingTokenHash;
    }

    public Instant getOnboardingTokenExpiresAt() {
        return onboardingTokenExpiresAt;
    }

    public Instant getOnboardingTokenConsumedAt() {
        return onboardingTokenConsumedAt;
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank()
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }
}
