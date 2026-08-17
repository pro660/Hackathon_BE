package org.likelionhsu.hackathon.styleplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.styleplan.domain.StyleItemRole;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanCreateRequest;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanPersistenceRepository;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StylePlanServiceTest {

    @Mock
    private StylePlanPersistenceRepository
            persistenceRepository;
    @Mock
    private AiJobJdbcRepository aiJobRepository;
    @Mock
    private UserItemRepository userItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StylePlanPreviewSourceValidator
            previewSourceValidator;

    private StylePlanService service;

    @BeforeEach
    void setUp() {
        service = new StylePlanService(
                persistenceRepository,
                aiJobRepository,
                userItemRepository,
                productRepository,
                previewSourceValidator
        );
    }

    @Test
    void aiPreviewCanBeSavedAfterRevalidation() {
        StylePlanCreateRequest request =
                request(9001L);

        AiJobData job = mock(AiJobData.class);
        UserItem item = mock(UserItem.class);
        Product product = mock(Product.class);

        when(persistenceRepository
                .existsByAiJobId(9001L))
                .thenReturn(false);
        when(aiJobRepository.findOwned(
                1L,
                9001L
        )).thenReturn(Optional.of(job));
        when(previewSourceValidator.validate(
                job,
                request
        )).thenReturn(
                StylePlanGenerationType.AI
        );
        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        501L,
                        1L
                )
        ).thenReturn(Optional.of(item));
        when(productRepository.findByIdAndStatus(
                101L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));
        when(product.getBrand())
                .thenReturn(ProductBrand.MCM);
        when(persistenceRepository.insertPlan(
                1L,
                request.title(),
                request.occasion(),
                request.plannedAt(),
                request.weatherCondition(),
                request.description(),
                StylePlanGenerationType.AI,
                request.status(),
                9001L
        )).thenReturn(601L);

        var response = service.create(
                1L,
                request
        );

        assertThat(response.stylePlanId())
                .isEqualTo("601");

        verify(persistenceRepository).insertItem(
                601L,
                501L,
                StyleItemRole.BAG,
                0
        );
        verify(persistenceRepository).insertProduct(
                601L,
                101L,
                1,
                "전체 색상 톤과 잘 어울려요."
        );
    }

    @Test
    void manualPlanUsesManualGenerationType() {
        StylePlanCreateRequest request =
                request(null);

        UserItem item = mock(UserItem.class);
        Product product = mock(Product.class);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        501L,
                        1L
                )
        ).thenReturn(Optional.of(item));
        when(productRepository.findByIdAndStatus(
                101L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));
        when(product.getBrand())
                .thenReturn(ProductBrand.MCM);
        when(persistenceRepository.insertPlan(
                1L,
                request.title(),
                request.occasion(),
                request.plannedAt(),
                request.weatherCondition(),
                request.description(),
                StylePlanGenerationType.MANUAL,
                request.status(),
                null
        )).thenReturn(602L);

        assertThat(
                service.create(1L, request)
                        .stylePlanId()
        ).isEqualTo("602");
    }

    @Test
    void deletedOrOtherUsersItemIsRejected() {
        StylePlanCreateRequest request =
                request(null);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        501L,
                        1L
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.create(1L, request)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException)
                                        exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.MY_ITEM_NOT_FOUND
                        )
                );
    }

    @Test
    void nonMcmProductIsRejected() {
        StylePlanCreateRequest request =
                request(null);

        UserItem item = mock(UserItem.class);
        Product product = mock(Product.class);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        501L,
                        1L
                )
        ).thenReturn(Optional.of(item));
        when(productRepository.findByIdAndStatus(
                101L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));
        when(product.getBrand())
                .thenReturn(ProductBrand.OTHER);

        assertThatThrownBy(() ->
                service.create(1L, request)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException)
                                        exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );
    }

    private StylePlanCreateRequest request(
            Long aiJobId
    ) {
        return new StylePlanCreateRequest(
                aiJobId,
                "데이트 룩",
                StylePlanOccasion.DATE,
                null,
                null,
                "깔끔한 보유 아이템 중심",
                StylePlanStatus.CONFIRMED,
                List.of(
                        new StylePlanCreateRequest
                                .OwnedItem(
                                501L,
                                StyleItemRole.BAG,
                                0
                        )
                ),
                List.of(
                        new StylePlanCreateRequest
                                .RecommendedProduct(
                                101L,
                                1,
                                "전체 색상 톤과 잘 어울려요."
                        )
                )
        );
    }
}
