package org.likelionhsu.hackathon.purchaseutility.repository;

import java.util.Optional;

import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@Repository
public class PurchaseUtilityAnalysisRepository {

    private final EntityManager entityManager;

    public PurchaseUtilityAnalysisRepository(
            EntityManager entityManager
    ) {
        this.entityManager = entityManager;
    }

    @Transactional
    public PurchaseUtilityAnalysis save(
            PurchaseUtilityAnalysis analysis
    ) {
        if (analysis.getId() == null) {
            entityManager.persist(analysis);
            return analysis;
        }

        return entityManager.merge(analysis);
    }

    @Transactional
    public PurchaseUtilityAnalysis saveAndFlush(
            PurchaseUtilityAnalysis analysis
    ) {
        PurchaseUtilityAnalysis saved = save(analysis);
        entityManager.flush();
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<PurchaseUtilityAnalysis> findByIdAndUser_Id(
            Long analysisId,
            Long userId
    ) {
        return entityManager.createQuery(
                        """
                        SELECT analysis
                        FROM PurchaseUtilityAnalysis analysis
                        WHERE analysis.id = :analysisId
                          AND analysis.user.id = :userId
                        """,
                        PurchaseUtilityAnalysis.class
                )
                .setParameter("analysisId", analysisId)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }
}
