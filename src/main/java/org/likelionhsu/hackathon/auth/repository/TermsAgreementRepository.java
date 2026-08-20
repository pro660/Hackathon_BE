package org.likelionhsu.hackathon.auth.repository;

import java.util.List;
import java.util.Optional;

import org.likelionhsu.hackathon.auth.domain.TermsAgreement;
import org.likelionhsu.hackathon.auth.domain.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAgreementRepository extends
        JpaRepository<TermsAgreement, Long> {

    List<TermsAgreement> findAllByUserId(Long userId);

    Optional<TermsAgreement>
    findTopByUserIdAndTermsTypeOrderByIdDesc(
            Long userId,
            TermsType termsType
    );
}
