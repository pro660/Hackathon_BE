package org.likelionhsu.hackathon.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.place.client.ExternalPlace;
import org.likelionhsu.hackathon.place.client.PlaceSearchException;
import org.likelionhsu.hackathon.place.client.PlaceSearchException.FailureKind;
import org.likelionhsu.hackathon.place.client.PlaceSearchPort;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.repository.PlaceRepository;
import org.likelionhsu.hackathon.place.repository.PlaceRepository.StoredPlace;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceSearchPort placeSearchPort;

    @Mock
    private PlaceRepository placeRepository;

    private PlaceService placeService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(placeSearchPort, placeRepository);
    }

    @Test
    void searchUpsertsKakaoPlacesAndMarksSaved() {
        ExternalPlace external = new ExternalPlace(
                "kakao-100",
                "성수 카페",
                PlaceCategory.CAFE,
                "음식점 > 카페",
                "서울 성동구 성수동",
                "서울 성동구 성수이로",
                new BigDecimal("37.5412000"),
                new BigDecimal("127.0563000"),
                "https://place.map.kakao.com/100"
        );

        when(placeSearchPort.search(any())).thenReturn(List.of(external));
        when(placeRepository.upsert(external)).thenReturn(
                new StoredPlace(
                        1001L,
                        external.name(),
                        external.category(),
                        external.categoryName(),
                        external.address(),
                        external.roadAddress(),
                        external.latitude(),
                        external.longitude(),
                        external.placeUrl()
                )
        );
        when(placeRepository.findSavedPlaceIds(
                eq(1L),
                eq(List.of(1001L))
        )).thenReturn(Set.of(1001L));

        var response = placeService.search(
                1L,
                "성수",
                PlaceCategory.CAFE,
                new BigDecimal("37.5445"),
                new BigDecimal("127.0560"),
                3000
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().placeId()).isEqualTo("1001");
        assertThat(response.items().getFirst().category()).isEqualTo(PlaceCategory.CAFE);
        assertThat(response.items().getFirst().saved()).isTrue();
    }

    @Test
    void coordinateOnlySearchRequiresCategory() {
        assertThatThrownBy(() -> placeService.search(
                1L,
                null,
                null,
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                3000
        )).isInstanceOf(RequestValidationException.class);
    }

    @Test
    void latitudeAndLongitudeMustBePaired() {
        assertThatThrownBy(() -> placeService.search(
                1L,
                "성수",
                PlaceCategory.CAFE,
                new BigDecimal("37.5"),
                null,
                null
        )).isInstanceOf(RequestValidationException.class);
    }

    @Test
    void providerTimeoutMapsTo504ErrorCode() {
        when(placeSearchPort.search(any())).thenThrow(
                new PlaceSearchException(FailureKind.TIMEOUT, "timeout")
        );

        assertThatThrownBy(() -> placeService.search(
                1L,
                "성수",
                PlaceCategory.CAFE,
                null,
                null,
                null
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.PLACE_PROVIDER_TIMEOUT)
        );
    }
}
