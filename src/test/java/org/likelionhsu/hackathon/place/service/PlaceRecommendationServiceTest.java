package org.likelionhsu.hackathon.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.place.client.ExternalPlace;
import org.likelionhsu.hackathon.place.client.PlaceSearchPort;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.dto.PlaceRecommendationRequest;
import org.likelionhsu.hackathon.place.repository.PlaceRepository;
import org.likelionhsu.hackathon.place.repository.PlaceRepository.StoredPlace;
import org.likelionhsu.hackathon.place.repository.StylePlanPlaceRepository;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanQueryRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceRecommendationServiceTest {

    @Mock
    PlaceSearchPort placeSearchPort;

    @Mock
    PlaceRepository placeRepository;

    @Mock
    StylePlanPlaceRepository stylePlanPlaceRepository;

    @Mock
    StylePlanQueryRepository stylePlanQueryRepository;

    PlaceRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new PlaceRecommendationService(
                placeSearchPort,
                placeRepository,
                stylePlanPlaceRepository,
                stylePlanQueryRepository
        );
    }

    @Test
    void dateRanksOccasionCategoryAndDistanceAndReplacesLinks() {
        when(stylePlanQueryRepository.findHeader(1L, 601L))
                .thenReturn(Optional.of(header(
                        StylePlanOccasion.DATE
                )));

        ExternalPlace cafe = external(
                "k1",
                "가까운 카페",
                PlaceCategory.CAFE,
                "37.5446",
                "127.0560"
        );
        ExternalPlace attraction = external(
                "k2",
                "가까운 명소",
                PlaceCategory.ATTRACTION,
                "37.5446",
                "127.0560"
        );

        when(placeSearchPort.search(any()))
                .thenReturn(List.of(cafe, attraction));

        when(placeRepository.upsert(cafe))
                .thenReturn(stored(1001L, cafe));
        when(placeRepository.upsert(attraction))
                .thenReturn(stored(1002L, attraction));
        when(placeRepository.findSavedPlaceIds(
                eq(1L),
                any()
        )).thenReturn(Set.of(1001L));

        var response = service.recommend(
                1L,
                601L,
                new PlaceRecommendationRequest(
                        new BigDecimal("37.5445"),
                        new BigDecimal("127.0560"),
                        3000,
                        null,
                        "성수"
                )
        );

        assertThat(response.rankingPolicyVersion())
                .isEqualTo("place-ranking-v1");
        assertThat(response.places()).hasSize(2);
        assertThat(response.places().getFirst().place().placeId())
                .isEqualTo("1001");
        assertThat(response.places().getFirst()
                .scoreBreakdown().categorySuitability())
                .isEqualTo(60.0);
        assertThat(response.places().getFirst().place().saved())
                .isTrue();

        verify(stylePlanPlaceRepository).replace(
                eq(601L),
                any()
        );
    }

    @Test
    void unknownPlanReturnsStylePlanNotFound() {
        when(stylePlanQueryRepository.findHeader(1L, 999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recommend(
                1L,
                999L,
                new PlaceRecommendationRequest(
                        new BigDecimal("37.5"),
                        new BigDecimal("127.0"),
                        null,
                        null,
                        null
                )
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.STYLE_PLAN_NOT_FOUND)
        );
    }

    @Test
    void emptyCandidatesReplaceWithEmptyList() {
        when(stylePlanQueryRepository.findHeader(1L, 601L))
                .thenReturn(Optional.of(header(
                        StylePlanOccasion.OUTDOOR
                )));
        when(placeSearchPort.search(any())).thenReturn(List.of());

        var response = service.recommend(
                1L,
                601L,
                new PlaceRecommendationRequest(
                        new BigDecimal("37.5"),
                        new BigDecimal("127.0"),
                        null,
                        null,
                        null
                )
        );

        assertThat(response.places()).isEmpty();
        verify(stylePlanPlaceRepository).replace(
                601L,
                List.of()
        );
    }

    private StylePlanQueryRepository.Header header(
            StylePlanOccasion occasion
    ) {
        Instant now = Instant.parse("2026-08-18T00:00:00Z");
        return new StylePlanQueryRepository.Header(
                601L,
                "데이트 룩",
                occasion,
                null,
                null,
                "설명",
                StylePlanGenerationType.AI,
                StylePlanStatus.CONFIRMED,
                0L,
                now,
                now
        );
    }

    private ExternalPlace external(
            String providerId,
            String name,
            PlaceCategory category,
            String latitude,
            String longitude
    ) {
        return new ExternalPlace(
                providerId,
                name,
                category,
                "테스트 > " + category.name(),
                "서울",
                "서울 도로명",
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                "https://place.map.kakao.com/" + providerId
        );
    }

    private StoredPlace stored(
            long id,
            ExternalPlace place
    ) {
        return new StoredPlace(
                id,
                place.name(),
                place.category(),
                place.categoryName(),
                place.address(),
                place.roadAddress(),
                place.latitude(),
                place.longitude(),
                place.placeUrl()
        );
    }
}
