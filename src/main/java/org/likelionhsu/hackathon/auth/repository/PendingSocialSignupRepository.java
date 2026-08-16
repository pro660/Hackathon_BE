package org.likelionhsu.hackathon.auth.repository;

import java.util.Optional;

import org.likelionhsu.hackathon.auth.domain.PendingSocialSignup;
import org.likelionhsu.hackathon.auth.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PendingSocialSignupRepository extends
        JpaRepository<PendingSocialSignup, Long> {

    Optional<PendingSocialSignup> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pending
            from PendingSocialSignup pending
            where pending.onboardingTokenHash = :tokenHash
            """)
    Optional<PendingSocialSignup> findByOnboardingTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}
