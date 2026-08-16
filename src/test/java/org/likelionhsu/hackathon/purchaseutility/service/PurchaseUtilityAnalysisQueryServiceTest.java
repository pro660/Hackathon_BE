package org.likelionhsu.hackathon.purchaseutility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityExplanationGenerationType;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityCompatibleItemSnapshot;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;
import org.likelionhsu.hackathon.purchaseutility.repository.PurchaseUtilityAnalysisRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseUtilityAnalysisQueryServiceTest {

    @Mock PurchaseUtilityAnalysisRepository analysisRepository;
    @Mock ProductImageRepository productImageRepository;

    private PurchaseUtilityAnalysisQueryService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseUtilityAnalysisQueryService(
                analysisRepository,
                productImageRepository
        );
    }

    @Test
    void detailUsesStoredCompatibleItemSnapshotAndCurrentProductImage() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(101L);
        when(product.getName()).thenReturn("Aren Shopper");
        when(product.getCategory()).thenReturn(ItemCategory.BAG);
        when(product.getPrice()).thenReturn(1_450_000L);

        PurchaseUtilityFactorSnapshot factors = factorSnapshot();

        PurchaseUtilityAnalysis analysis =
                mock(PurchaseUtilityAnalysis.class);
        when(analysis.getId()).thenReturn(801L);
        when(analysis.getProduct()).thenReturn(product);
        when(analysis.getUtilityScore())
                .thenReturn(new BigDecimal("77.00"));
        when(analysis.getCompatibleItemCount()).thenReturn(2);
        when(analysis.getFactorJson()).thenReturn(factors);
        when(analysis.getSummary()).thenReturn("활용도가 높은 편입니다.");
        when(analysis.getAnalyzedAt())
                .thenReturn(
                        Instant.parse("2026-08-16T10:30:00Z")
                );

        ProductImage laterImage = mock(ProductImage.class);
        when(laterImage.getSortOrder()).thenReturn(5);

        ProductImage primaryImage = mock(ProductImage.class);
        when(primaryImage.getSortOrder()).thenReturn(0);
        when(primaryImage.getUrl())
                .thenReturn("https://example.com/product.webp");

        when(analysisRepository.findByIdAndUser_Id(801L, 1L))
                .thenReturn(Optional.of(analysis));
        when(productImageRepository
                .findAllByProduct_IdInAndPrimaryTrue(
                        List.of(101L)
                ))
                .thenReturn(
                        List.of(
                                laterImage,
                                primaryImage
                        )
                );

        var response = service.getAnalysis(1L, 801L);

        assertThat(response.analysisId()).isEqualTo("801");
        assertThat(response.scorePolicyVersion())
                .isEqualTo("purchase-utility-rule-v1");
        assertThat(response.product().productId())
                .isEqualTo("101");
        assertThat(response.product().name())
                .isEqualTo("Aren Shopper");
        assertThat(response.product().primaryImageUrl())
                .isEqualTo("https://example.com/product.webp");
        assertThat(response.utilityScore())
                .isEqualByComparingTo("77.00");
        assertThat(response.factors().preferenceTagFitScore())
                .isEqualByComparingTo("20.00");
        assertThat(response.factors().styleCombinationScore())
                .isEqualByComparingTo("18.00");
        assertThat(response.factors().seasonUsabilityScore())
                .isEqualByComparingTo("25.00");
        assertThat(response.factors().ownedCategoryCombinationScore())
                .isEqualByComparingTo("14.00");
        assertThat(response.compatibleItemCount()).isEqualTo(2);
        assertThat(response.compatibleItems()).hasSize(2);
        assertThat(response.compatibleItems().getFirst().myItemId())
                .isEqualTo("501");
        assertThat(response.compatibleItems().getFirst().name())
                .isEqualTo("베이지 재킷");
        assertThat(response.compatibleItems().getFirst().imageUrl())
                .isEqualTo("https://example.com/item.webp");
        assertThat(response.explanationGenerationType())
                .isEqualTo(
                        PurchaseUtilityExplanationGenerationType.RULE_BASED
                );
        assertThat(response.analyzedAt())
                .isEqualTo(
                        Instant.parse("2026-08-16T10:30:00Z")
                );
    }

    @Test
    void missingOrOtherUsersAnalysisReturnsSameNotFound() {
        when(analysisRepository.findByIdAndUser_Id(999L, 1L))
                .thenReturn(Optional.empty());

        BusinessException exception =
                catchThrowableOfType(
                        () -> service.getAnalysis(1L, 999L),
                        BusinessException.class
                );

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        ErrorCode.PURCHASE_UTILITY_ANALYSIS_NOT_FOUND
                );
        verifyNoInteractions(productImageRepository);
    }

    @Test
    void detailAllowsMissingProductImage() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(101L);
        when(product.getName()).thenReturn("Aren Shopper");
        when(product.getCategory()).thenReturn(ItemCategory.BAG);
        when(product.getPrice()).thenReturn(1_450_000L);

        PurchaseUtilityAnalysis analysis =
                mock(PurchaseUtilityAnalysis.class);
        when(analysis.getId()).thenReturn(801L);
        when(analysis.getProduct()).thenReturn(product);
        when(analysis.getUtilityScore())
                .thenReturn(new BigDecimal("77.00"));
        when(analysis.getCompatibleItemCount()).thenReturn(2);
        when(analysis.getFactorJson()).thenReturn(factorSnapshot());
        when(analysis.getSummary()).thenReturn("활용도가 높은 편입니다.");
        when(analysis.getAnalyzedAt())
                .thenReturn(
                        Instant.parse("2026-08-16T10:30:00Z")
                );

        when(analysisRepository.findByIdAndUser_Id(801L, 1L))
                .thenReturn(Optional.of(analysis));
        when(productImageRepository
                .findAllByProduct_IdInAndPrimaryTrue(
                        List.of(101L)
                ))
                .thenReturn(List.of());

        var response = service.getAnalysis(1L, 801L);

        assertThat(response.product().primaryImageUrl()).isNull();
    }

    private PurchaseUtilityFactorSnapshot factorSnapshot() {
        return new PurchaseUtilityFactorSnapshot(
                "purchase-utility-rule-v1",
                PurchaseUtilityExplanationGenerationType.RULE_BASED,
                new PurchaseUtilityFactorSnapshot.PreferenceFactor(
                        new BigDecimal("20.00"),
                        new BigDecimal("30.00"),
                        true,
                        true,
                        false
                ),
                new PurchaseUtilityFactorSnapshot.ItemCombinationFactor(
                        new BigDecimal("18.00"),
                        new BigDecimal("25.00"),
                        2,
                        List.of(
                                new PurchaseUtilityCompatibleItemSnapshot(
                                        "501",
                                        "베이지 재킷",
                                        ItemCategory.CLOTHING,
                                        ColorGroup.BEIGE,
                                        "https://example.com/item.webp",
                                        "구매 후보 제품과 색상 조합이 가능한 아이템입니다."
                                ),
                                new PurchaseUtilityCompatibleItemSnapshot(
                                        "502",
                                        "블랙 로퍼",
                                        ItemCategory.SHOES,
                                        ColorGroup.BLACK,
                                        null,
                                        "구매 후보 제품과 색상 조합이 가능한 아이템입니다."
                                )
                        )
                ),
                new PurchaseUtilityFactorSnapshot.SeasonFactor(
                        new BigDecimal("25.00"),
                        new BigDecimal("25.00"),
                        4,
                        true
                ),
                new PurchaseUtilityFactorSnapshot.CategoryCombinationFactor(
                        new BigDecimal("14.00"),
                        new BigDecimal("20.00"),
                        2
                )
        );
    }
}
