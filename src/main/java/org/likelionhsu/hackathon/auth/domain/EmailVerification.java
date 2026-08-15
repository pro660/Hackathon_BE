package org.likelionhsu.hackathon.auth.domain;

import java.time.Instant;

import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verifications")
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailVerificationPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "code_expires_at", nullable = false)
    private Instant codeExpiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(
            name = "signup_token_hash",
            unique = true,
            length = 64
    )
    private String signupTokenHash;

    @Column(name = "signup_token_expires_at")
    private Instant signupTokenExpiresAt;

    @Column(name = "signup_token_consumed_at")
    private Instant signupTokenConsumedAt;

    protected EmailVerification() {
    }

    public static EmailVerification signup(
            String email,
            String codeHash,
            Instant codeExpiresAt
    ) {
        EmailVerification verification = new EmailVerification();
        verification.user = null;
        verification.email = email.trim()
                .toLowerCase(java.util.Locale.ROOT);
        verification.purpose = EmailVerificationPurpose.SIGNUP;
        verification.codeHash = codeHash;
        verification.attemptCount = 0;
        verification.codeExpiresAt = codeExpiresAt;
        return verification;
    }

    public void recordFailedAttempt() {
        attemptCount++;
    }

    public void completeSignupVerification(
            String signupTokenHash,
            Instant verifiedAt,
            Instant signupTokenExpiresAt
    ) {
        this.verifiedAt = verifiedAt;
        this.signupTokenHash = signupTokenHash;
        this.signupTokenExpiresAt = signupTokenExpiresAt;
    }

    public void consumeSignupToken(Instant consumedAt) {
        this.signupTokenConsumedAt = consumedAt;
    }

    public boolean isCodeUsableAt(Instant now, int maxAttempts) {
        return verifiedAt == null
                && attemptCount < maxAttempts
                && codeExpiresAt.isAfter(now);
    }

    public boolean isSignupTokenUsableAt(Instant now) {
        return verifiedAt != null
                && signupTokenHash != null
                && signupTokenConsumedAt == null
                && signupTokenExpiresAt != null
                && signupTokenExpiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getCodeExpiresAt() {
        return codeExpiresAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public String getSignupTokenHash() {
        return signupTokenHash;
    }

    public Instant getSignupTokenExpiresAt() {
        return signupTokenExpiresAt;
    }

    public Instant getSignupTokenConsumedAt() {
        return signupTokenConsumedAt;
    }
}
