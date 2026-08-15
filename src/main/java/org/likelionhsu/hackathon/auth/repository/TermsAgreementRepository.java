package org.likelionhsu.hackathon.auth.repository;

import java.util.List;

import org.likelionhsu.hackathon.auth.domain.TermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAgreementRepository extends
        JpaRepository<TermsAgreement, Long> {

    List<TermsAgreement> findAllByUserId(Long userId);
}
