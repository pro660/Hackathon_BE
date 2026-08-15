package org.likelionhsu.hackathon.recommendation.repository;

import java.util.Optional;

import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository
        extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findByIdAndUser_Id(
            Long recommendationId,
            Long userId
    );
}
