package org.likelionhsu.hackathon.useritem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.itemanalysis.ai.ItemAnalysisResult;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemCreateRequest;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemUpdateRequest;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemCreateResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemDetailResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemListItemResponse;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemAiJobValidator;
import org.likelionhsu.hackathon.useritem.repository.UserItemAiJobValidator.ItemAnalysisProvenance;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageData;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserItemServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant NOW =
            Instant.parse("2026-08-16T00:00:00Z");

    private static final String VALID_INPUT_HASH =
            "a".repeat(64);

    @Mock
    UserItemRepository userItemRepository;

    @Mock
    UserItemImageRepository userItemImageRepository;

    @Mock
    UserItemAiJobValidator userItemAiJobValidator;

    @Mock
    UserRepository userRepository;

    @Mock
    ProductRepository productRepository;

    UserItemService userItemService;

    @BeforeEach
    void setUp() {
        userItemService = new UserItemService(
                userItemRepository,
                userItemImageRepository,
                userItemAiJobValidator,
                userRepository,
                productRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void itemCanBeCreatedWithoutBrand() {
        User user = user();

        when(userRepository.getReferenceById(USER_ID))
                .thenReturn(user);

        when(userItemRepository.save(any(UserItem.class)))
                .thenAnswer(invocation -> {
                    UserItem item = invocation.getArgument(0);
                    ReflectionTestUtils.setField(item, "id", 10L);
                    return item;
                });

        UserItemCreateResponse response =
                userItemService.createMyItem(
                        USER_ID,
                        new UserItemCreateRequest(
                                null,
                                null,
                                " 브라운 토트백 ",
                                ItemCategory.BAG,
                                ColorGroup.BROWN,
                                MaterialGroup.LEATHER,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                );

        assertThat(response.myItemId()).isEqualTo("10");

        verify(userItemRepository).save(
                argThat(item ->
                        item.getBrandName() == null
                )
        );
        verify(userItemAiJobValidator, never())
                .validateOwnedSucceededItemAnalysis(any(), any());
    }

    @Test
    void itemListUsesPrimaryItemImages() {
        UserItem item = item(10L, 0L);
        PageRequest pageable = PageRequest.of(0, 20);

        when(userItemRepository.findAll(
                any(Specification.class),
                any(PageRequest.class)
        ))
                .thenReturn(
                        new PageImpl<>(
                                List.of(item),
                                pageable,
                                1
                        )
                );

        when(userItemImageRepository.findPrimaryImageUrls(
                USER_ID,
                List.of(10L)
        )).thenReturn(
                Map.of(
                        10L,
                        "https://example.com/item.webp"
                )
        );

        PageResponse<UserItemListItemResponse> response =
                userItemService.getMyItems(
                        USER_ID,
                        "토트",
                        ItemCategory.BAG,
                        ColorGroup.BROWN,
                        pageable
                );

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.items().getFirst().myItemId())
                .isEqualTo("10");
        assertThat(response.items().getFirst().primaryImageUrl())
                .isEqualTo("https://example.com/item.webp");
    }

    @Test
    void itemDetailReturnsOrderedImages() {
        UserItem item = item(10L, 2L);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        when(userItemImageRepository.findActiveImages(
                USER_ID,
                10L
        )).thenReturn(
                List.of(
                        new UserItemImageData(
                                100L,
                                10L,
                                "https://example.com/item.webp",
                                0
                        )
                )
        );

        UserItemDetailResponse response =
                userItemService.getMyItem(USER_ID, 10L);

        assertThat(response.myItemId()).isEqualTo("10");
        assertThat(response.images()).hasSize(1);
        assertThat(response.images().getFirst().imageId())
                .isEqualTo("100");
        assertThat(response.version()).isEqualTo(2L);
    }

    @Test
    void anotherUsersItemIsHiddenAsNotFound() {
        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        999L,
                        USER_ID
                )).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> userItemService.getMyItem(USER_ID, 999L)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(ErrorCode.MY_ITEM_NOT_FOUND)
                );
    }

    @Test
    void staleVersionIsRejected() {
        UserItem item = item(10L, 3L);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setName("수정 이름");
        request.setVersion(2L);

        assertThatThrownBy(
                () -> userItemService.updateMyItem(
                        USER_ID,
                        10L,
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

        verify(userItemRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void nullablePatchFieldCanBeCleared() {
        UserItem item = item(10L, 2L);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        when(userItemImageRepository.findActiveImages(
                USER_ID,
                10L
        )).thenReturn(List.of());

        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setMemo(null);
        request.setVersion(2L);

        UserItemDetailResponse response =
                userItemService.updateMyItem(
                        USER_ID,
                        10L,
                        request
                );

        assertThat(response.memo()).isNull();
        verify(userItemRepository).saveAndFlush(item);
    }

    @Test
    void brandNameCanBeClearedWithExplicitNull() {
        UserItem item = item(10L, 2L);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        when(userItemImageRepository.findActiveImages(
                USER_ID,
                10L
        )).thenReturn(List.of());

        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setBrandName(null);
        request.setVersion(2L);

        UserItemDetailResponse response =
                userItemService.updateMyItem(
                        USER_ID,
                        10L,
                        request
                );

        assertThat(response.brandName()).isNull();
        verify(userItemRepository).saveAndFlush(item);
    }

    @Test
    void aiEstimatedMaterialMatchingAnalysisCanBeCreated() {
        when(userItemAiJobValidator
                .validateOwnedSucceededItemAnalysis(
                        USER_ID,
                        77L
                ))
                .thenReturn(
                        provenance(MaterialGroup.LEATHER)
                );

        stubCreateSave(11L);

        UserItemCreateResponse response =
                userItemService.createMyItem(
                        USER_ID,
                        createRequest(
                                null,
                                MaterialGroup.LEATHER,
                                MaterialSource.AI_ESTIMATED,
                                77L
                        )
                );

        assertThat(response.myItemId()).isEqualTo("11");

        verify(userItemRepository).save(
                argThat(item ->
                        item.getMaterial()
                                == MaterialGroup.LEATHER
                                && item.getMaterialSource()
                                == MaterialSource.AI_ESTIMATED
                                && item.getAiJobId()
                                .equals(77L)
                )
        );
    }

    @Test
    void aiEstimatedMaterialMismatchIsRejected() {
        when(userItemAiJobValidator
                .validateOwnedSucceededItemAnalysis(
                        USER_ID,
                        77L
                ))
                .thenReturn(
                        provenance(MaterialGroup.LEATHER)
                );

        assertThatThrownBy(
                () -> userItemService.createMyItem(
                        USER_ID,
                        createRequest(
                                null,
                                MaterialGroup.CANVAS,
                                MaterialSource.AI_ESTIMATED,
                                77L
                        )
                )
        )
                .isInstanceOf(RequestValidationException.class)
                .satisfies(exception -> {
                    RequestValidationException validation =
                            (RequestValidationException) exception;

                    assertThat(validation.getField())
                            .isEqualTo("material");
                    assertThat(validation.getReason())
                            .isEqualTo(
                                    "아이템 분석 결과의 소재와 일치해야 합니다."
                            );
                });

        verify(userItemRepository, never())
                .save(any());
    }

    @Test
    void productDataMaterialMatchingProductCanBeCreated() {
        Product product =
                product(20L, MaterialGroup.LEATHER);

        when(productRepository.findByIdAndStatus(
                20L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));

        stubCreateSave(12L);

        UserItemCreateResponse response =
                userItemService.createMyItem(
                        USER_ID,
                        createRequest(
                                20L,
                                MaterialGroup.LEATHER,
                                MaterialSource.PRODUCT_DATA,
                                null
                        )
                );

        assertThat(response.myItemId()).isEqualTo("12");

        verify(userItemRepository).save(
                argThat(item ->
                        item.getMaterial()
                                == MaterialGroup.LEATHER
                                && item.getMaterialSource()
                                == MaterialSource.PRODUCT_DATA
                                && item.getProduct()
                                == product
                )
        );
    }

    @Test
    void productDataMaterialMismatchIsRejected() {
        Product product =
                product(20L, MaterialGroup.LEATHER);

        when(productRepository.findByIdAndStatus(
                20L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));

        assertThatThrownBy(
                () -> userItemService.createMyItem(
                        USER_ID,
                        createRequest(
                                20L,
                                MaterialGroup.CANVAS,
                                MaterialSource.PRODUCT_DATA,
                                null
                        )
                )
        )
                .isInstanceOf(RequestValidationException.class)
                .satisfies(exception -> {
                    RequestValidationException validation =
                            (RequestValidationException) exception;

                    assertThat(validation.getField())
                            .isEqualTo("material");
                    assertThat(validation.getReason())
                            .isEqualTo(
                                    "연결된 제품의 소재와 일치해야 합니다."
                            );
                });

        verify(userItemRepository, never())
                .save(any());
    }

    @Test
    void changingMaterialWithoutSourceBecomesUserConfirmed() {
        UserItem item = itemWithState(
                10L,
                2L,
                null,
                MaterialGroup.LEATHER,
                MaterialSource.AI_ESTIMATED,
                77L
        );

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        when(userItemImageRepository.findActiveImages(
                USER_ID,
                10L
        )).thenReturn(List.of());

        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setMaterial(MaterialGroup.CANVAS);
        request.setVersion(2L);

        UserItemDetailResponse response =
                userItemService.updateMyItem(
                        USER_ID,
                        10L,
                        request
                );

        assertThat(response.material())
                .isEqualTo(MaterialGroup.CANVAS);
        assertThat(response.materialSource())
                .isEqualTo(MaterialSource.USER_CONFIRMED);

        verify(userItemAiJobValidator, never())
                .validateOwnedSucceededItemAnalysis(
                        any(),
                        any()
                );
    }

    @Test
    void changingLinkedProductDowngradesProductDataToUserConfirmed() {
        Product oldProduct =
                product(10L, MaterialGroup.LEATHER);
        Product newProduct =
                product(20L, MaterialGroup.CANVAS);

        UserItem item = itemWithState(
                10L,
                2L,
                oldProduct,
                MaterialGroup.LEATHER,
                MaterialSource.PRODUCT_DATA,
                null
        );

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        when(productRepository.findByIdAndStatus(
                20L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(newProduct));

        when(userItemImageRepository.findActiveImages(
                USER_ID,
                10L
        )).thenReturn(List.of());

        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setProductId(20L);
        request.setVersion(2L);

        UserItemDetailResponse response =
                userItemService.updateMyItem(
                        USER_ID,
                        10L,
                        request
                );

        assertThat(response.material())
                .isEqualTo(MaterialGroup.LEATHER);
        assertThat(response.materialSource())
                .isEqualTo(MaterialSource.USER_CONFIRMED);
        assertThat(response.linkedProductId())
                .isEqualTo("20");
    }

    @Test
    void resendingSameMaterialKeepsAiEstimated() {
        UserItem item = itemWithState(
                10L,
                2L,
                null,
                MaterialGroup.LEATHER,
                MaterialSource.AI_ESTIMATED,
                77L
        );

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        when(userItemAiJobValidator
                .validateOwnedSucceededItemAnalysis(
                        USER_ID,
                        77L
                ))
                .thenReturn(
                        provenance(MaterialGroup.LEATHER)
                );

        when(userItemImageRepository.findActiveImages(
                USER_ID,
                10L
        )).thenReturn(List.of());

        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setMaterial(MaterialGroup.LEATHER);
        request.setVersion(2L);

        UserItemDetailResponse response =
                userItemService.updateMyItem(
                        USER_ID,
                        10L,
                        request
                );

        assertThat(response.material())
                .isEqualTo(MaterialGroup.LEATHER);
        assertThat(response.materialSource())
                .isEqualTo(MaterialSource.AI_ESTIMATED);

        verify(userItemAiJobValidator)
                .validateOwnedSucceededItemAnalysis(
                        USER_ID,
                        77L
                );
    }

    @Test
    void resendingSameProductKeepsProductData() {
        Product product =
                product(10L, MaterialGroup.LEATHER);

        UserItem item = itemWithState(
                10L,
                2L,
                product,
                MaterialGroup.LEATHER,
                MaterialSource.PRODUCT_DATA,
                null
        );

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        10L,
                        USER_ID
                )).thenReturn(Optional.of(item));

        when(productRepository.findByIdAndStatus(
                10L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));

        when(userItemImageRepository.findActiveImages(
                USER_ID,
                10L
        )).thenReturn(List.of());

        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setProductId(10L);
        request.setVersion(2L);

        UserItemDetailResponse response =
                userItemService.updateMyItem(
                        USER_ID,
                        10L,
                        request
                );

        assertThat(response.materialSource())
                .isEqualTo(MaterialSource.PRODUCT_DATA);
        assertThat(response.linkedProductId())
                .isEqualTo("10");
    }

    @Test
    void aiJobIdCannotBeChangedAfterCreation() {
        UserItemUpdateRequest request =
                new UserItemUpdateRequest();
        request.setAiJobId(null);
        request.setVersion(2L);

        assertThatThrownBy(
                () -> userItemService.updateMyItem(
                        USER_ID,
                        10L,
                        request
                )
        )
                .isInstanceOf(RequestValidationException.class)
                .satisfies(exception -> {
                    RequestValidationException validation =
                            (RequestValidationException) exception;

                    assertThat(validation.getField())
                            .isEqualTo("aiJobId");
                    assertThat(validation.getReason())
                            .isEqualTo(
                                    "마이 아이템 생성 후에는 변경할 수 없습니다."
                            );
                });

        verify(userItemRepository, never())
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        any(),
                        any()
                );
    }

    @Test
    void itemDeleteSoftDeletesAndMarksImagesPending() {
        UserItem item = item(10L, 0L);

        when(userItemRepository.findByIdAndUser_Id(
                10L,
                USER_ID
        )).thenReturn(Optional.of(item));

        userItemService.deleteMyItem(USER_ID, 10L);

        assertThat(item.getDeletedAt()).isEqualTo(NOW);
        verify(userItemImageRepository)
                .markDeletePending(USER_ID, 10L);
        verify(userItemRepository).saveAndFlush(item);
    }

    @Test
    void repeatedDeleteIsIdempotent() {
        UserItem item = item(10L, 1L);
        ReflectionTestUtils.setField(item, "deletedAt", NOW);

        when(userItemRepository.findByIdAndUser_Id(
                10L,
                USER_ID
        )).thenReturn(Optional.of(item));

        userItemService.deleteMyItem(USER_ID, 10L);

        verify(userItemImageRepository, never())
                .markDeletePending(any(), any());
        verify(userItemRepository, never())
                .saveAndFlush(any());
    }

    private UserItemCreateRequest createRequest(
            Long productId,
            MaterialGroup material,
            MaterialSource materialSource,
            Long aiJobId
    ) {
        return new UserItemCreateRequest(
                productId,
                null,
                "테스트 아이템",
                ItemCategory.BAG,
                ColorGroup.BROWN,
                material,
                materialSource,
                null,
                null,
                null,
                aiJobId,
                null
        );
    }

    private ItemAnalysisProvenance provenance(
            MaterialGroup material
    ) {
        return new ItemAnalysisProvenance(
                VALID_INPUT_HASH,
                new ItemAnalysisResult(
                        "MCM",
                        "분석 아이템",
                        ItemCategory.BAG,
                        ColorGroup.BROWN,
                        material
                )
        );
    }

    private void stubCreateSave(Long itemId) {
        when(userRepository.getReferenceById(USER_ID))
                .thenReturn(user());

        when(userItemRepository.save(any(UserItem.class)))
                .thenAnswer(invocation -> {
                    UserItem item = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            item,
                            "id",
                            itemId
                    );
                    return item;
                });
    }

    private Product product(
            Long id,
            MaterialGroup material
    ) {
        Product product = Product.create(
                ProductBrand.MCM,
                "SKU-" + id,
                "테스트 제품",
                ItemCategory.BAG,
                null,
                100000L,
                ColorGroup.BROWN,
                material,
                null,
                ProductStatus.ACTIVE
        );

        ReflectionTestUtils.setField(
                product,
                "id",
                id
        );

        return product;
    }

    private UserItem itemWithState(
            Long id,
            Long version,
            Product product,
            MaterialGroup material,
            MaterialSource materialSource,
            Long aiJobId
    ) {
        UserItem item = UserItem.create(
                user(),
                product,
                "MCM",
                "브라운 토트백",
                ItemCategory.BAG,
                ColorGroup.BROWN,
                material,
                materialSource,
                null,
                null,
                "메모",
                aiJobId,
                null
        );

        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(
                item,
                "version",
                version
        );
        ReflectionTestUtils.setField(
                item,
                "createdAt",
                NOW
        );
        ReflectionTestUtils.setField(
                item,
                "updatedAt",
                NOW
        );

        return item;
    }

    private User user() {
        User user = User.local(
                "item@example.com",
                "아이템사용자",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private UserItem item(Long id, Long version) {
        UserItem item = UserItem.create(
                user(),
                null,
                "MCM",
                "브라운 토트백",
                ItemCategory.BAG,
                ColorGroup.BROWN,
                MaterialGroup.LEATHER,
                MaterialSource.USER_CONFIRMED,
                null,
                null,
                "메모",
                null,
                null
        );

        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "version", version);
        ReflectionTestUtils.setField(item, "createdAt", NOW);
        ReflectionTestUtils.setField(item, "updatedAt", NOW);
        return item;
    }
}
