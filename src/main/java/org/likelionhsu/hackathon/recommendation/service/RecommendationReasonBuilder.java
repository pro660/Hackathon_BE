package org.likelionhsu.hackathon.recommendation.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationFeature;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationOccasion;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationSeason;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationScoreBreakdownSnapshot;
import org.likelionhsu.hackathon.recommendation.service.RecommendationScorer.RecommendationScoreResult;
import org.likelionhsu.hackathon.recommendation.validation.RecommendationRequestValidator.ValidatedRecommendationRequest;
import org.springframework.stereotype.Component;

@Component
public class RecommendationReasonBuilder {

    public String buildReason(
            RecommendationScoreResult scoreResult,
            RecommendationOccasion occasion,
            RecommendationSeason season
    ) {
        RecommendationScoreBreakdownSnapshot breakdown =
                scoreResult.breakdown();
        List<String> reasons = new ArrayList<>();

        if (isPositive(breakdown.style())) {
            reasons.add("선호 스타일");
        }

        if (isPositive(breakdown.occasion())) {
            reasons.add(occasion.label() + " 상황");
        }

        if (isPositive(breakdown.season())) {
            reasons.add(
                    scoreResult.allSeasonOnlyMatch()
                            ? "사계절 활용"
                            : season.label() + " 시즌"
            );
        }

        if (isPositive(breakdown.feature())) {
            String featureReason =
                    scoreResult.matchedFeatures()
                            .stream()
                            .map(RecommendationFeature::label)
                            .collect(Collectors.joining("·"));
            reasons.add(featureReason + " 특징");
        }

        return String.join(", ", reasons)
                + "에 잘 맞는 제품입니다.";
    }

    public String buildSummary(
            ValidatedRecommendationRequest request,
            int resultCount
    ) {
        if (resultCount == 0) {
            return "입력한 조건에 맞는 추천 제품을 찾지 못했습니다.";
        }

        String category =
                request.category() == null
                        ? "MCM 제품"
                        : categoryLabel(request.category())
                                + " MCM 제품";

        return request.occasion().label()
                + " 상황과 "
                + request.season().label()
                + " 시즌, 선택한 기능을 기준으로 취향에 맞는 "
                + category
                + "을 추천했어요.";
    }

    private boolean isPositive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String categoryLabel(ItemCategory category) {
        return switch (category) {
            case BAG -> "가방";
            case LEATHER_GOODS -> "가죽 소품";
            case FASHION_ACCESSORY -> "패션 액세서리";
            case CLOTHING -> "의류";
            case SHOES -> "신발";
        };
    }
}
