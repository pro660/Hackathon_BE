package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.styleplan.domain.StyleItemRole;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanDetailResponse;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanListItemResponse;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanPersistenceRepository;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanQueryRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class StylePlanQueryServiceTest {

    private StylePlanQueryRepository queryRepository;
    private StylePlanService service;

    @BeforeEach
    void setUp() {
        queryRepository = mock(StylePlanQueryRepository.class);
        service = new StylePlanService(
                mock(StylePlanPersistenceRepository.class),
                queryRepository,
                mock(AiJobJdbcRepository.class),
                mock(UserItemRepository.class),
                mock(ProductRepository.class),
                mock(StylePlanPreviewSourceValidator.class)
        );
    }

    @Test
    void listReturnsCommonPageResponse() {
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Order.desc("createdAt"))
        );

        StylePlanListItemResponse item = new StylePlanListItemResponse(
                "601",
                "데이트 룩",
                StylePlanOccasion.DATE,
                null,
                StylePlanStatus.CONFIRMED,
                "https://example.com/item.webp",
                2,
                1,
                Instant.parse("2026-08-18T00:00:00Z")
        );

        when(queryRepository.findPage(
                1L,
                StylePlanStatus.CONFIRMED,
                pageable
        )).thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        var response = service.getStylePlans(
                1L,
                StylePlanStatus.CONFIRMED,
                pageable
        );

        assertThat(response.items()).containsExactly(item);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.page()).isZero();
    }

    @Test
    void detailReturnsStoredComposition() {
        Instant now = Instant.parse("2026-08-18T00:00:00Z");

        when(queryRepository.findHeader(1L, 601L)).thenReturn(
                Optional.of(new StylePlanQueryRepository.Header(
                        601L,
                        "데이트 룩",
                        StylePlanOccasion.DATE,
                        null,
                        null,
                        "설명",
                        StylePlanGenerationType.AI,
                        StylePlanStatus.CONFIRMED,
                        0L,
                        now,
                        now
                ))
        );

        var ownedItem = new StylePlanDetailResponse.OwnedItem(
                "501",
                "브라운 데일리백",
                "https://example.com/item.webp",
                StyleItemRole.BAG,
                0
        );
        var product = new StylePlanDetailResponse.RecommendedProduct(
                "101",
                "Aren Shopper",
                "https://example.com/product.webp",
                1,
                "잘 어울려요."
        );

        when(queryRepository.findOwnedItems(1L, 601L))
                .thenReturn(List.of(ownedItem));
        when(queryRepository.findRecommendedProducts(601L))
                .thenReturn(List.of(product));

        StylePlanDetailResponse response = service.getStylePlan(1L, 601L);

        assertThat(response.stylePlanId()).isEqualTo("601");
        assertThat(response.ownedItems()).containsExactly(ownedItem);
        assertThat(response.recommendedProducts()).containsExactly(product);
        assertThat(response.places()).isEmpty();
        assertThat(response.version()).isZero();
    }

    @Test
    void missingOrOtherUsersPlanReturnsNotFound() {
        when(queryRepository.findHeader(1L, 999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStylePlan(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.STYLE_PLAN_NOT_FOUND));
    }
}
