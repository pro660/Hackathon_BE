package org.likelionhsu.hackathon.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.likelionhsu.hackathon.auth.domain.EmailVerification;
import org.likelionhsu.hackathon.auth.domain.EmailVerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface EmailVerificationRepository extends
        JpaRepository<EmailVerification, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification>
    findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email,
            EmailVerificationPurpose purpose
    );

    long countByEmailAndPurposeAndCreatedAtGreaterThanEqual(
            String email,
            EmailVerificationPurpose purpose,
            Instant since
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select verification
            from EmailVerification verification
            where verification.signupTokenHash = :tokenHash
            """)
    Optional<EmailVerification> findBySignupTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}
