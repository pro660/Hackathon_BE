package org.likelionhsu.hackathon.auth.repository;

import java.util.Optional;

import org.likelionhsu.hackathon.auth.domain.SocialAccount;
import org.likelionhsu.hackathon.auth.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialAccountRepository extends
        JpaRepository<SocialAccount, Long> {

    @Query("""
            select account
            from SocialAccount account
            join fetch account.user
            where account.provider = :provider
              and account.providerUserId = :providerUserId
            """)
    Optional<SocialAccount> findWithUserByProviderAndProviderUserId(
            @Param("provider") SocialProvider provider,
            @Param("providerUserId") String providerUserId
    );

    boolean existsByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    boolean existsByProviderEmailIgnoreCase(String providerEmail);
}
