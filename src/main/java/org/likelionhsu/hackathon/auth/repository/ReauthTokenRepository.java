package org.likelionhsu.hackathon.auth.repository;

import java.util.Optional;

import org.likelionhsu.hackathon.auth.domain.ReauthToken;
import org.likelionhsu.hackathon.auth.domain.ReauthTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ReauthTokenRepository extends
        JpaRepository<ReauthToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from ReauthToken token
            join fetch token.user
            where token.tokenHash = :tokenHash
            """)
    Optional<ReauthToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    void deleteByUser_IdAndPurpose(
            Long userId,
            ReauthTokenPurpose purpose
    );
}
