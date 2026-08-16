package org.likelionhsu.hackathon.purchaseutility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.preference.entity.PreferenceStyleTag;
import org.likelionhsu.hackathon.preference.repository.PreferenceRepository;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTag;
import org.likelionhsu.hackathon.product.entity.ProductTagMapping;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.likelionhsu.hackathon.purchaseutility.domain.CareDifficulty;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.repository.PurchaseUtilityAnalysisRepository;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityAnalysisService.RuleAnalysisStatus;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PurchaseUtilityAnalysisServiceTest {

    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock PreferenceRepository preferenceRepository;
    @Mock UserItemRepository userItemRepository;
    @Mock UserItemImageRepository userItemImageRepository;
    @Mock ProductTagMappingRepository productTagMappingRepository;
    @Mock PurchaseUtilityAnalysisRepository analysisRepository;

    private PurchaseUtilityAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseUtilityAnalysisService(
                userRepository,
                productRepository,
                preferenceRepository,
                userItemRepository,
                userItemImageRepository,
                productTagMappingRepository,
                analysisRepository,
                new PurchaseUtilityScorer(),
                Clock.fixed(
                        Instant.parse("2026-08-16T00:00:00Z"),
                        ZoneOffset.UTC
                )
        );
    }

    @Test
    void readyAnalysisUsesCurrentDataAndPersistsSnapshot() {
        User user = activeUser();
        Product product = activeProduct(
                100L,
                ItemCategory.BAG,
                ColorGroup.RED
        );
        when(product.getMaterial())
                .thenReturn(MaterialGroup.LEATHER);
        PreferenceProfile preference =
                preference(
                        List.of(PreferenceStyleTag.CASUAL),
                        List.of(ItemCategory.BAG),
                        List.of(ColorGroup.RED)
                );

        UserItem beigeJacket =
                userItem(
                        501L,
                        "베이지 재킷",
                        ItemCategory.CLOTHING,
                        ColorGroup.BEIGE
                );
        UserItem blackShoes =
                userItem(
                        502L,
                        "블랙 로퍼",
                        ItemCategory.SHOES,
                        ColorGroup.BLACK
                );
        UserItem blueAccessory =
                userItem(
                        503L,
                        "블루 액세서리",
                        ItemCategory.FASHION_ACCESSORY,
                        ColorGroup.BLUE
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatus(
                100L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));
        when(preferenceRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(preference));
        when(userItemRepository.findAll(
                ArgumentMatchers
                        .<Specification<UserItem>>any()
        )).thenReturn(
                List.of(
                        blueAccessory,
                        blackShoes,
                        beigeJacket
                )
        );
        List<ProductTagMapping> productTags =
                List.of(
                        tag(
                                ProductTagType.SEASON,
                                "AUTUMN"
                        ),
                        tag(
                                ProductTagType.STYLE,
                                "CASUAL"
                        ),
                        tag(
                                ProductTagType.SEASON,
                                "SPRING"
                        )
                );
        when(productTagMappingRepository
                .findAllWithTagByProductId(100L))
                .thenReturn(productTags);
        when(userItemImageRepository.findPrimaryImageUrls(
                1L,
                List.of(501L, 502L, 503L)
        )).thenReturn(
                Map.of(
                        501L,
                        "https://example.com/beige.webp"
                )
        );
        doAnswer(invocation -> invocation.getArgument(0))
                .when(analysisRepository)
                .save(any(PurchaseUtilityAnalysis.class));

        var result = service.createRuleBasedAnalysis(
                1L,
                100L,
                700L
        );

        assertThat(result.status())
                .isEqualTo(RuleAnalysisStatus.READY);
        assertThat(result.isReady()).isTrue();
        assertThat(result.message()).isNull();

        PurchaseUtilityAnalysis analysis =
                result.analysis();

        assertThat(analysis.getUtilityScore())
                .isEqualByComparingTo("83.00");
        assertThat(analysis.getCompatibleItemCount())
                .isEqualTo(2);
        assertThat(analysis.getCareDifficulty())
                .isEqualTo(CareDifficulty.HARD);
        assertThat(analysis.getSummary())
                .isEqualTo(
                        "현재 보유 아이템 및 취향과의 활용 가능성이 높은 제품입니다. "
                                + "보유 아이템 중 2개와 조합할 수 있으며, "
                                + "관리 난이도는 어려운 편입니다."
                );
        assertThat(analysis.getAiJobId())
                .isEqualTo(700L);
        assertThat(analysis.getAnalyzedAt())
                .isEqualTo(
                        Instant.parse(
                                "2026-08-16T00:00:00Z"
                        )
                );
        assertThat(
                analysis
                        .getFactorJson()
                        .itemCombination()
                        .compatibleItems()
        ).extracting(
                item -> item.myItemId()
        ).containsExactly(
                "501",
                "502"
        );
        assertThat(
                analysis
                        .getFactorJson()
                        .itemCombination()
                        .compatibleItems()
                        .getFirst()
                        .imageUrl()
        ).isEqualTo(
                "https://example.com/beige.webp"
        );
        assertThat(
                analysis
                        .getFactorJson()
                        .itemCombination()
                        .compatibleItems()
                        .get(1)
                        .imageUrl()
        ).isNull();

        verify(productRepository)
                .findByIdAndStatus(
                        100L,
                        ProductStatus.ACTIVE
                );
        verify(userItemImageRepository)
                .findPrimaryImageUrls(
                        1L,
                        List.of(501L, 502L, 503L)
                );
        verify(analysisRepository)
                .save(any(PurchaseUtilityAnalysis.class));
    }

    @Test
    void missingPreferenceReturnsInsufficientWithoutSaving() {
        User user = activeUser();
        Product product = activeProduct(
                100L,
                ItemCategory.BAG,
                ColorGroup.BLACK
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatus(
                100L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));
        when(preferenceRepository.findByUser_Id(1L))
                .thenReturn(Optional.empty());

        var result = service.createRuleBasedAnalysis(
                1L,
                100L,
                700L
        );

        assertThat(result.status())
                .isEqualTo(
                        RuleAnalysisStatus.INSUFFICIENT_DATA
                );
        assertThat(result.analysis()).isNull();
        assertThat(result.message())
                .isEqualTo(
                        "활용 가능성을 분석하기 위한 정보가 부족해요."
                );

        verifyNoInteractions(
                userItemRepository,
                userItemImageRepository,
                productTagMappingRepository,
                analysisRepository
        );
    }

    @Test
    void noActiveUserItemsReturnsInsufficientWithoutSaving() {
        User user = activeUser();
        Product product = activeProduct(
                100L,
                ItemCategory.BAG,
                ColorGroup.BLACK
        );
        PreferenceProfile preference =
                preference(
                        List.of(PreferenceStyleTag.CASUAL),
                        List.of(ItemCategory.BAG),
                        List.of(ColorGroup.BLACK)
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatus(
                100L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));
        when(preferenceRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(preference));
        when(userItemRepository.findAll(
                ArgumentMatchers
                        .<Specification<UserItem>>any()
        )).thenReturn(List.of());

        var result = service.createRuleBasedAnalysis(
                1L,
                100L,
                null
        );

        assertThat(result.status())
                .isEqualTo(
                        RuleAnalysisStatus.INSUFFICIENT_DATA
                );

        verifyNoInteractions(
                userItemImageRepository,
                productTagMappingRepository,
                analysisRepository
        );
    }

    @Test
    void missingRequiredProductTagsReturnsInsufficientWithoutSaving() {
        User user = activeUser();
        Product product = activeProduct(
                100L,
                ItemCategory.BAG,
                ColorGroup.BLACK
        );
        PreferenceProfile preference =
                preference(
                        List.of(PreferenceStyleTag.CASUAL),
                        List.of(ItemCategory.BAG),
                        List.of(ColorGroup.BLACK)
                );
        UserItem item =
                userItem(
                        501L,
                        "화이트 셔츠",
                        ItemCategory.CLOTHING,
                        ColorGroup.WHITE
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatus(
                100L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));
        when(preferenceRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(preference));
        when(userItemRepository.findAll(
                ArgumentMatchers
                        .<Specification<UserItem>>any()
        )).thenReturn(List.of(item));
        List<ProductTagMapping> productTags =
                List.of(
                        tag(
                                ProductTagType.SEASON,
                                "AUTUMN"
                        )
                );
        when(productTagMappingRepository
                .findAllWithTagByProductId(100L))
                .thenReturn(productTags);

        var result = service.createRuleBasedAnalysis(
                1L,
                100L,
                null
        );

        assertThat(result.status())
                .isEqualTo(
                        RuleAnalysisStatus.INSUFFICIENT_DATA
                );

        verifyNoInteractions(
                userItemImageRepository,
                analysisRepository
        );
    }

    @Test
    void productWithoutPrimaryColorReturnsInsufficientEarly() {
        User user = activeUser();
        Product product = activeProduct(
                100L,
                ItemCategory.BAG,
                null
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatus(
                100L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.of(product));

        var result = service.createRuleBasedAnalysis(
                1L,
                100L,
                null
        );

        assertThat(result.status())
                .isEqualTo(
                        RuleAnalysisStatus.INSUFFICIENT_DATA
                );

        verifyNoInteractions(
                preferenceRepository,
                userItemRepository,
                userItemImageRepository,
                productTagMappingRepository,
                analysisRepository
        );
    }

    @Test
    void missingOrInactiveProductUsesExistingNotFoundPolicy() {
        User user = activeUser();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatus(
                999L,
                ProductStatus.ACTIVE
        )).thenReturn(Optional.empty());

        BusinessException exception =
                catchThrowableOfType(
                        () ->
                                service.createRuleBasedAnalysis(
                                        1L,
                                        999L,
                                        null
                                ),
                        BusinessException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

        verifyNoInteractions(
                preferenceRepository,
                userItemRepository,
                userItemImageRepository,
                productTagMappingRepository,
                analysisRepository
        );
    }

    private User activeUser() {
        User user = mock(User.class);
        when(user.getStatus())
                .thenReturn(UserStatus.ACTIVE);
        return user;
    }

    private Product activeProduct(
            Long id,
            ItemCategory category,
            ColorGroup color
    ) {
        Product product = mock(Product.class);
        lenient().when(product.getId()).thenReturn(id);
        lenient().when(product.getCategory()).thenReturn(category);
        lenient().when(product.getPrimaryColor()).thenReturn(color);
        return product;
    }

    private PreferenceProfile preference(
            List<PreferenceStyleTag> styleTags,
            List<ItemCategory> categories,
            List<ColorGroup> colors
    ) {
        PreferenceProfile preference =
                mock(PreferenceProfile.class);
        lenient().when(preference.getPreferredStyleTags())
                .thenReturn(styleTags);
        lenient().when(preference.getPreferredCategories())
                .thenReturn(categories);
        lenient().when(preference.getPreferredColors())
                .thenReturn(colors);
        return preference;
    }

    private UserItem userItem(
            Long id,
            String name,
            ItemCategory category,
            ColorGroup color
    ) {
        UserItem item = mock(UserItem.class);
        lenient().when(item.getId()).thenReturn(id);
        lenient().when(item.getName()).thenReturn(name);
        lenient().when(item.getCategory()).thenReturn(category);
        lenient().when(item.getPrimaryColor()).thenReturn(color);
        return item;
    }

    private ProductTagMapping tag(
            ProductTagType type,
            String code
    ) {
        ProductTag productTag = mock(ProductTag.class);
        when(productTag.getType()).thenReturn(type);
        when(productTag.getCode()).thenReturn(code);

        ProductTagMapping mapping =
                mock(ProductTagMapping.class);
        when(mapping.getProductTag())
                .thenReturn(productTag);
        return mapping;
    }
}
