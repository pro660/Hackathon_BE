package org.likelionhsu.hackathon.auth.repository;

import java.util.Optional;

import org.likelionhsu.hackathon.auth.domain.LocalCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface LocalCredentialRepository extends
        JpaRepository<LocalCredential, Long> {

    boolean existsByLoginId(String loginId);

    @Query("""
            select credential
            from LocalCredential credential
            join fetch credential.user
            where credential.loginId = :loginId
            """)
    Optional<LocalCredential> findWithUserByLoginId(
            @Param("loginId") String loginId
    );

    @Query("""
            select credential
            from LocalCredential credential
            join fetch credential.user
            where credential.user.id = :userId
            """)
    Optional<LocalCredential> findWithUserByUserId(
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select credential
            from LocalCredential credential
            join fetch credential.user
            where credential.user.id = :userId
            """)
    Optional<LocalCredential> findWithUserByUserIdForUpdate(
            @Param("userId") Long userId
    );

    boolean existsByUser_Id(Long userId);
}
