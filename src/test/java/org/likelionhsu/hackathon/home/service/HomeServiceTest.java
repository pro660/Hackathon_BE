package org.likelionhsu.hackathon.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.home.repository.HomeQueryRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    HomeQueryRepository homeQueryRepository;

    HomeService service;

    @BeforeEach
    void setUp() {
        service = new HomeService(homeQueryRepository);
    }

    @Test
    void aggregatesOnlyStoredHomeData() {
        when(homeQueryRepository.findUserSummary(1L))
                .thenReturn(Optional.of(
                        new HomeQueryRepository.UserSummaryRow(
                                "오늘뭐입지", "ACTIVE", true, 8L
                        )
                ));
        when(homeQueryRepository.findLatestStylePlan(1L))
                .thenReturn(Optional.of(
                        new HomeQueryRepository.LatestStylePlanRow(
                                601L,
                                "데이트 룩",
                                "https://example.com/item.webp"
                        )
                ));
        when(homeQueryRepository.findLatestRecommendedProducts(1L))
                .thenReturn(List.of(
                        new HomeQueryRepository.RecommendedProductRow(
                                101L,
                                "Aren Shopper",
                                new BigDecimal("82.00"),
                                "https://example.com/product.webp"
                        )
                ));

        var response = service.getHome(1L);

        assertThat(response.user().nickname()).isEqualTo("오늘뭐입지");
        assertThat(response.user().preferenceCompleted()).isTrue();
        assertThat(response.user().myItemCount()).isEqualTo(8L);
        assertThat(response.latestStylePlan().stylePlanId()).isEqualTo("601");
        assertThat(response.recommendedProducts()).singleElement()
                .satisfies(product -> {
                    assertThat(product.productId()).isEqualTo("101");
                    assertThat(product.matchScore()).isEqualByComparingTo("82.00");
                });
    }

    @Test
    void missingOptionalSectionsAreNullAndEmpty() {
        when(homeQueryRepository.findUserSummary(1L))
                .thenReturn(Optional.of(
                        new HomeQueryRepository.UserSummaryRow(
                                "새사용자", "ACTIVE", false, 0L
                        )
                ));
        when(homeQueryRepository.findLatestStylePlan(1L))
                .thenReturn(Optional.empty());
        when(homeQueryRepository.findLatestRecommendedProducts(1L))
                .thenReturn(List.of());

        var response = service.getHome(1L);

        assertThat(response.latestStylePlan()).isNull();
        assertThat(response.recommendedProducts()).isEmpty();
    }

    @Test
    void inactiveUserIsRejected() {
        when(homeQueryRepository.findUserSummary(1L))
                .thenReturn(Optional.of(
                        new HomeQueryRepository.UserSummaryRow(
                                "사용자", "DELETION_PENDING", true, 1L
                        )
                ));

        assertThatThrownBy(() -> service.getHome(1L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVE)
                );
    }
}
