package org.likelionhsu.hackathon.place.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.place.client.ExternalPlace;
import org.likelionhsu.hackathon.place.client.PlaceSearchCommand;
import org.likelionhsu.hackathon.place.client.PlaceSearchException;
import org.likelionhsu.hackathon.place.client.PlaceSearchPort;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.dto.PlaceRecommendationRequest;
import org.likelionhsu.hackathon.place.dto.PlaceRecommendationResponse;
import org.likelionhsu.hackathon.place.repository.PlaceRepository;
import org.likelionhsu.hackathon.place.repository.PlaceRepository.StoredPlace;
import org.likelionhsu.hackathon.place.repository.StylePlanPlaceRepository;
import org.likelionhsu.hackathon.place.repository.StylePlanPlaceRepository.StylePlanPlaceLink;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PlaceRecommendationService {

    static final String RANKING_POLICY_VERSION = "place-ranking-v1";
    private static final int MAX_RECOMMENDATIONS = 3;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final PlaceSearchPort placeSearchPort;
    private final PlaceRepository placeRepository;
    private final StylePlanPlaceRepository stylePlanPlaceRepository;
    private final StylePlanQueryRepository stylePlanQueryRepository;

    public PlaceRecommendationService(
            PlaceSearchPort placeSearchPort,
            PlaceRepository placeRepository,
            StylePlanPlaceRepository stylePlanPlaceRepository,
            StylePlanQueryRepository stylePlanQueryRepository
    ) {
        this.placeSearchPort = placeSearchPort;
        this.placeRepository = placeRepository;
        this.stylePlanPlaceRepository = stylePlanPlaceRepository;
        this.stylePlanQueryRepository = stylePlanQueryRepository;
    }

    public PlaceRecommendationResponse recommend(
            Long userId,
            Long stylePlanId,
            PlaceRecommendationRequest request
    ) {
        var header = stylePlanQueryRepository
                .findHeader(userId, stylePlanId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.STYLE_PLAN_NOT_FOUND
                ));

        validateRequest(request);

        String query = StringUtils.hasText(request.query())
                ? request.query().trim()
                : null;
        int radius = request.effectiveRadius();

        List<ExternalPlace> externalPlaces = searchCandidates(
                header.occasion(),
                query,
                request.category(),
                request.latitude(),
                request.longitude(),
                radius
        );

        List<Candidate> candidates = new ArrayList<>();
        int sourceOrder = 0;

        for (ExternalPlace external : externalPlaces) {
            double distanceMeters = distanceMeters(
                    request.latitude(),
                    request.longitude(),
                    external.latitude(),
                    external.longitude()
            );

            if (distanceMeters > radius) {
                sourceOrder++;
                continue;
            }

            StoredPlace stored = placeRepository.upsert(external);
            double categoryScore = categoryScore(
                    header.occasion(),
                    external.category()
            );
            double distanceScore = 40.0
                    * Math.max(0.0, 1.0 - distanceMeters / radius);

            candidates.add(new Candidate(
                    stored,
                    categoryScore,
                    distanceScore,
                    distanceMeters,
                    sourceOrder
            ));
            sourceOrder++;
        }

        List<Candidate> ranked = candidates.stream()
                .sorted(Comparator
                        .comparingDouble(Candidate::totalScore)
                        .reversed()
                        .thenComparingDouble(Candidate::distanceMeters)
                        .thenComparingInt(Candidate::sourceOrder))
                .limit(MAX_RECOMMENDATIONS)
                .toList();

        Set<Long> savedIds = placeRepository.findSavedPlaceIds(
                userId,
                ranked.stream()
                        .map(candidate -> candidate.place().id())
                        .toList()
        );

        List<PlaceRecommendationResponse.RecommendedPlace> responsePlaces =
                new ArrayList<>();
        List<StylePlanPlaceLink> links = new ArrayList<>();

        for (int index = 0; index < ranked.size(); index++) {
            Candidate candidate = ranked.get(index);
            int rank = index + 1;
            String reasonCode = reasonCode(candidate);

            responsePlaces.add(
                    toResponse(
                            rank,
                            candidate,
                            reasonCode,
                            savedIds.contains(candidate.place().id())
                    )
            );
            links.add(new StylePlanPlaceLink(
                    candidate.place().id(),
                    rank,
                    reasonCode
            ));
        }

        stylePlanPlaceRepository.replace(stylePlanId, links);

        return new PlaceRecommendationResponse(
                String.valueOf(stylePlanId),
                RANKING_POLICY_VERSION,
                responsePlaces
        );
    }

    private void validateRequest(PlaceRecommendationRequest request) {
        if (request.category() == PlaceCategory.OTHER) {
            throw new RequestValidationException(
                    "category",
                    "OTHER는 장소 추천 조건으로 사용할 수 없습니다."
            );
        }
    }

    private List<ExternalPlace> searchCandidates(
            StylePlanOccasion occasion,
            String query,
            PlaceCategory requestedCategory,
            BigDecimal latitude,
            BigDecimal longitude,
            int radius
    ) {
        List<PlaceCategory> categories;

        if (requestedCategory != null) {
            categories = List.of(requestedCategory);
        } else if (query != null) {
            categories = List.of();
        } else {
            categories = preferredCategories(occasion);
        }

        Map<String, ExternalPlace> deduplicated = new LinkedHashMap<>();

        if (categories.isEmpty()) {
            addSearchResults(
                    deduplicated,
                    new PlaceSearchCommand(
                            query,
                            null,
                            latitude,
                            longitude,
                            radius
                    )
            );
        } else {
            for (PlaceCategory category : categories) {
                addSearchResults(
                        deduplicated,
                        new PlaceSearchCommand(
                                query,
                                category,
                                latitude,
                                longitude,
                                radius
                        )
                );
            }
        }

        return List.copyOf(deduplicated.values());
    }

    private void addSearchResults(
            Map<String, ExternalPlace> deduplicated,
            PlaceSearchCommand command
    ) {
        List<ExternalPlace> results;

        try {
            results = placeSearchPort.search(command);
        } catch (PlaceSearchException exception) {
            if (exception.failureKind()
                    == PlaceSearchException.FailureKind.TIMEOUT) {
                throw new BusinessException(
                        ErrorCode.PLACE_PROVIDER_TIMEOUT
                );
            }
            throw new BusinessException(
                    ErrorCode.PLACE_PROVIDER_UNAVAILABLE
            );
        }

        for (ExternalPlace result : results) {
            deduplicated.putIfAbsent(
                    result.providerPlaceId(),
                    result
            );
        }
    }

    private List<PlaceCategory> preferredCategories(
            StylePlanOccasion occasion
    ) {
        return switch (occasion) {
            case DAILY -> List.of(
                    PlaceCategory.CAFE,
                    PlaceCategory.RESTAURANT
            );
            case DATE -> List.of(
                    PlaceCategory.CAFE,
                    PlaceCategory.RESTAURANT,
                    PlaceCategory.CULTURE
            );
            case TRAVEL -> List.of(
                    PlaceCategory.ATTRACTION,
                    PlaceCategory.CULTURE,
                    PlaceCategory.RESTAURANT
            );
            case GATHERING -> List.of(
                    PlaceCategory.RESTAURANT,
                    PlaceCategory.CAFE
            );
            case CEREMONY -> List.of(
                    PlaceCategory.RESTAURANT
            );
            case OUTDOOR -> List.of(
                    PlaceCategory.ATTRACTION
            );
            case OTHER -> List.of(
                    PlaceCategory.CAFE,
                    PlaceCategory.RESTAURANT,
                    PlaceCategory.CULTURE,
                    PlaceCategory.ATTRACTION,
                    PlaceCategory.SHOPPING
            );
        };
    }

    private double categoryScore(
            StylePlanOccasion occasion,
            PlaceCategory category
    ) {
        if (occasion == StylePlanOccasion.OTHER) {
            return 60.0;
        }

        return preferredCategories(occasion).contains(category)
                ? 60.0
                : 0.0;
    }

    private PlaceRecommendationResponse.RecommendedPlace toResponse(
            int rank,
            Candidate candidate,
            String reasonCode,
            boolean saved
    ) {
        StoredPlace place = candidate.place();

        return new PlaceRecommendationResponse.RecommendedPlace(
                rank,
                roundOne(candidate.totalScore()),
                new PlaceRecommendationResponse.ScoreBreakdown(
                        roundOne(candidate.categoryScore()),
                        roundOne(candidate.distanceScore())
                ),
                reasonCode,
                new PlaceRecommendationResponse.Place(
                        String.valueOf(place.id()),
                        place.name(),
                        place.category(),
                        place.categoryName(),
                        place.roadAddress(),
                        place.latitude(),
                        place.longitude(),
                        place.placeUrl(),
                        saved
                )
        );
    }

    private String reasonCode(Candidate candidate) {
        return candidate.categoryScore() > 0
                ? "OCCASION_CATEGORY_AND_DISTANCE_MATCH"
                : "DISTANCE_MATCH";
    }

    private double roundOne(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double distanceMeters(
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude
    ) {
        double lat1 = Math.toRadians(fromLatitude.doubleValue());
        double lon1 = Math.toRadians(fromLongitude.doubleValue());
        double lat2 = Math.toRadians(toLatitude.doubleValue());
        double lon2 = Math.toRadians(toLongitude.doubleValue());

        double latDelta = lat2 - lat1;
        double lonDelta = lon2 - lon1;

        double haversine = Math.pow(Math.sin(latDelta / 2.0), 2.0)
                + Math.cos(lat1)
                * Math.cos(lat2)
                * Math.pow(Math.sin(lonDelta / 2.0), 2.0);

        return 2.0 * EARTH_RADIUS_METERS
                * Math.asin(Math.sqrt(haversine));
    }

    private record Candidate(
            StoredPlace place,
            double categoryScore,
            double distanceScore,
            double distanceMeters,
            int sourceOrder
    ) {
        double totalScore() {
            return categoryScore + distanceScore;
        }
    }
}
