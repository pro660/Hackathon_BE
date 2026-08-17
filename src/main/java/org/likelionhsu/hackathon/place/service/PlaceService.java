package org.likelionhsu.hackathon.place.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.place.client.ExternalPlace;
import org.likelionhsu.hackathon.place.client.PlaceSearchCommand;
import org.likelionhsu.hackathon.place.client.PlaceSearchException;
import org.likelionhsu.hackathon.place.client.PlaceSearchPort;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.dto.PlaceResponse;
import org.likelionhsu.hackathon.place.dto.PlaceSearchResponse;
import org.likelionhsu.hackathon.place.repository.PlaceRepository;
import org.likelionhsu.hackathon.place.repository.PlaceRepository.StoredPlace;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PlaceService {

    private static final int MAX_QUERY_LENGTH = 200;
    private static final int MAX_RESULTS = 15;

    private final PlaceSearchPort placeSearchPort;
    private final PlaceRepository placeRepository;

    public PlaceService(
            PlaceSearchPort placeSearchPort,
            PlaceRepository placeRepository
    ) {
        this.placeSearchPort = placeSearchPort;
        this.placeRepository = placeRepository;
    }

    public PlaceSearchResponse search(
            Long userId,
            String query,
            PlaceCategory category,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer radius
    ) {
        String normalizedQuery = normalizeAndValidate(
                query,
                category,
                latitude,
                longitude,
                radius
        );

        List<ExternalPlace> externalPlaces;
        try {
            externalPlaces = placeSearchPort.search(
                    new PlaceSearchCommand(
                            normalizedQuery,
                            category,
                            latitude,
                            longitude,
                            radius
                    )
            );
        } catch (PlaceSearchException exception) {
            if (exception.failureKind()
                    == PlaceSearchException.FailureKind.TIMEOUT) {
                throw new BusinessException(ErrorCode.PLACE_PROVIDER_TIMEOUT);
            }
            throw new BusinessException(ErrorCode.PLACE_PROVIDER_UNAVAILABLE);
        }

        List<StoredPlace> stored = externalPlaces.stream()
                .limit(MAX_RESULTS)
                .map(placeRepository::upsert)
                .toList();

        Set<Long> savedIds = placeRepository.findSavedPlaceIds(
                userId,
                stored.stream().map(StoredPlace::id).toList()
        );

        return new PlaceSearchResponse(
                stored.stream()
                        .map(place -> toResponse(
                                place,
                                savedIds.contains(place.id())
                        ))
                        .toList()
        );
    }

    private String normalizeAndValidate(
            String query,
            PlaceCategory category,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer radius
    ) {
        String normalizedQuery = StringUtils.hasText(query)
                ? query.trim()
                : null;

        if (normalizedQuery != null
                && normalizedQuery.length() > MAX_QUERY_LENGTH) {
            throw new RequestValidationException(
                    "query",
                    "query는 200자 이하여야 합니다."
            );
        }

        if (category == PlaceCategory.OTHER) {
            throw new RequestValidationException(
                    "category",
                    "OTHER는 장소 검색 조건으로 사용할 수 없습니다."
            );
        }

        boolean hasLatitude = latitude != null;
        boolean hasLongitude = longitude != null;
        if (hasLatitude != hasLongitude) {
            throw new RequestValidationException(
                    "latitude",
                    "latitude와 longitude는 함께 입력해야 합니다."
            );
        }

        if (hasLatitude) {
            if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                    || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new RequestValidationException(
                        "latitude",
                        "latitude는 -90 이상 90 이하여야 합니다."
                );
            }
            if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                    || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
                throw new RequestValidationException(
                        "longitude",
                        "longitude는 -180 이상 180 이하여야 합니다."
                );
            }
        }

        if (radius != null) {
            if (!hasLatitude) {
                throw new RequestValidationException(
                        "radius",
                        "radius를 사용하려면 latitude와 longitude가 필요합니다."
                );
            }
            if (radius < 1 || radius > 20_000) {
                throw new RequestValidationException(
                        "radius",
                        "radius는 1 이상 20000 이하여야 합니다."
                );
            }
        }

        if (normalizedQuery == null) {
            if (!hasLatitude || category == null) {
                throw new RequestValidationException(
                        "query",
                        "query 또는 latitude/longitude와 category 조합이 필요합니다."
                );
            }
        }

        return normalizedQuery;
    }

    private PlaceResponse toResponse(
            StoredPlace place,
            boolean saved
    ) {
        return new PlaceResponse(
                String.valueOf(place.id()),
                place.name(),
                place.category(),
                place.categoryName(),
                place.address(),
                place.roadAddress(),
                place.latitude(),
                place.longitude(),
                place.placeUrl(),
                saved
        );
    }
}
