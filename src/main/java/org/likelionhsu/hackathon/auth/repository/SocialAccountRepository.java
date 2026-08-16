package org.likelionhsu.hackathon.auth.repository;

import java.util.List;
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

    @Query("""
            select account.provider
            from SocialAccount account
            where account.user.id = :userId
            order by account.provider
            """)
    List<SocialProvider> findProvidersByUserId(
            @Param("userId") Long userId
    );
}
