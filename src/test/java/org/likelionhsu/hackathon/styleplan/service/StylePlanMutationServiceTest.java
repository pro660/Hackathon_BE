package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanUpdateRequest;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanPersistenceRepository;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanQueryRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StylePlanMutationServiceTest {

    @Mock
    private StylePlanPersistenceRepository persistenceRepository;
    @Mock
    private StylePlanQueryRepository queryRepository;
    @Mock
    private AiJobJdbcRepository aiJobRepository;
    @Mock
    private UserItemRepository userItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StylePlanPreviewSourceValidator previewSourceValidator;

    private StylePlanService service;

    @BeforeEach
    void setUp() {
        service = new StylePlanService(
                persistenceRepository,
                queryRepository,
                aiJobRepository,
                userItemRepository,
                productRepository,
                previewSourceValidator
        );
    }

    @Test
    void updateChangesOnlySubmittedMetadataAndIncrementsVersion() {
        StylePlanUpdateRequest request =
                new StylePlanUpdateRequest();
        request.setTitle("주말 데이트 룩");
        request.setPlannedAt(null);
        request.setStatus(StylePlanStatus.COMPLETED);
        request.setVersion(1L);

        var before = header(
                "데이트 룩",
                Instant.parse("2026-08-20T10:00:00Z"),
                StylePlanStatus.CONFIRMED,
                1L
        );

        var after = header(
                "주말 데이트 룩",
                null,
                StylePlanStatus.COMPLETED,
                2L
        );

        when(queryRepository.findHeader(1L, 601L))
                .thenReturn(
                        Optional.of(before),
                        Optional.of(after)
                );
        when(persistenceRepository.updateMetadata(
                1L,
                601L,
                "주말 데이트 룩",
                null,
                StylePlanStatus.COMPLETED,
                1L
        )).thenReturn(1);
        when(queryRepository.findOwnedItems(1L, 601L))
                .thenReturn(List.of());
        when(queryRepository.findRecommendedProducts(601L))
                .thenReturn(List.of());

        var response = service.updateStylePlan(
                1L,
                601L,
                request
        );

        assertThat(response.title())
                .isEqualTo("주말 데이트 룩");
        assertThat(response.plannedAt()).isNull();
        assertThat(response.status())
                .isEqualTo(StylePlanStatus.COMPLETED);
        assertThat(response.version())
                .isEqualTo(2L);
    }

    @Test
    void staleVersionReturnsResourceVersionConflict() {
        StylePlanUpdateRequest request =
                new StylePlanUpdateRequest();
        request.setTitle("수정");
        request.setVersion(1L);

        when(queryRepository.findHeader(1L, 601L))
                .thenReturn(Optional.of(
                        header(
                                "현재 제목",
                                null,
                                StylePlanStatus.CONFIRMED,
                                2L
                        )
                ));

        assertThatThrownBy(() ->
                service.updateStylePlan(
                        1L,
                        601L,
                        request
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.RESOURCE_VERSION_CONFLICT
                        )
                );
    }

    @Test
    void updateOfOtherUsersOrMissingPlanReturnsNotFound() {
        StylePlanUpdateRequest request =
                new StylePlanUpdateRequest();
        request.setTitle("수정");
        request.setVersion(0L);

        when(queryRepository.findHeader(1L, 999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateStylePlan(
                        1L,
                        999L,
                        request
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.STYLE_PLAN_NOT_FOUND
                        )
                );
    }

    @Test
    void requestWithoutEditableFieldIsRejected() {
        StylePlanUpdateRequest request =
                new StylePlanUpdateRequest();
        request.setVersion(0L);

        assertThatThrownBy(() ->
                service.updateStylePlan(
                        1L,
                        601L,
                        request
                )
        ).isInstanceOf(
                RequestValidationException.class
        );
    }

    @Test
    void deleteUsesOwnerScopedDelete() {
        when(persistenceRepository.deleteOwnedPlan(
                1L,
                601L
        )).thenReturn(1);

        service.deleteStylePlan(1L, 601L);

        verify(persistenceRepository)
                .deleteOwnedPlan(1L, 601L);
    }

    @Test
    void deleteOfOtherUsersOrMissingPlanReturnsNotFound() {
        when(persistenceRepository.deleteOwnedPlan(
                1L,
                999L
        )).thenReturn(0);

        assertThatThrownBy(() ->
                service.deleteStylePlan(1L, 999L)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.STYLE_PLAN_NOT_FOUND
                        )
                );
    }

    private StylePlanQueryRepository.Header header(
            String title,
            Instant plannedAt,
            StylePlanStatus status,
            long version
    ) {
        Instant now = Instant.parse(
                "2026-08-18T00:00:00Z"
        );

        return new StylePlanQueryRepository.Header(
                601L,
                title,
                StylePlanOccasion.DATE,
                plannedAt,
                null,
                "설명",
                StylePlanGenerationType.AI,
                status,
                version,
                now,
                now
        );
    }
}
