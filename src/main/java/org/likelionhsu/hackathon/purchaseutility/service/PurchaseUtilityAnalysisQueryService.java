package org.likelionhsu.hackathon.purchaseutility.service;

import java.util.Comparator;
import java.util.List;

import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.purchaseutility.dto.response.PurchaseUtilityAnalysisResponse;
import org.likelionhsu.hackathon.purchaseutility.dto.response.PurchaseUtilityAnalysisResponse.CompatibleItemResponse;
import org.likelionhsu.hackathon.purchaseutility.dto.response.PurchaseUtilityAnalysisResponse.FactorScoresResponse;
import org.likelionhsu.hackathon.purchaseutility.dto.response.PurchaseUtilityAnalysisResponse.ProductResponse;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.entity.snapshot.PurchaseUtilityFactorSnapshot;
import org.likelionhsu.hackathon.purchaseutility.repository.PurchaseUtilityAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseUtilityAnalysisQueryService {

    private final PurchaseUtilityAnalysisRepository analysisRepository;
    private final ProductImageRepository productImageRepository;

    public PurchaseUtilityAnalysisQueryService(
            PurchaseUtilityAnalysisRepository analysisRepository,
            ProductImageRepository productImageRepository
    ) {
        this.analysisRepository = analysisRepository;
        this.productImageRepository = productImageRepository;
    }

    @Transactional(readOnly = true)
    public PurchaseUtilityAnalysisResponse getAnalysis(
            Long userId,
            Long analysisId
    ) {
        PurchaseUtilityAnalysis analysis =
                analysisRepository
                        .findByIdAndUser_Id(
                                analysisId,
                                userId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.PURCHASE_UTILITY_ANALYSIS_NOT_FOUND
                                )
                        );

        Product product = analysis.getProduct();
        PurchaseUtilityFactorSnapshot factors =
                analysis.getFactorJson();

        List<CompatibleItemResponse> compatibleItems =
                factors
                        .itemCombination()
                        .compatibleItems()
                        .stream()
                        .map(item ->
                                new CompatibleItemResponse(
                                        item.myItemId(),
                                        item.name(),
                                        item.imageUrl(),
                                        item.reason()
                                )
                        )
                        .toList();

        return new PurchaseUtilityAnalysisResponse(
                String.valueOf(analysis.getId()),
                factors.ruleVersion(),
                new ProductResponse(
                        String.valueOf(product.getId()),
                        product.getName(),
                        product.getCategory(),
                        product.getPrice(),
                        findPrimaryImageUrl(product.getId())
                ),
                analysis.getUtilityScore(),
                new FactorScoresResponse(
                        factors.preference().score(),
                        factors.itemCombination().score(),
                        factors.season().score(),
                        factors.categoryCombination().score()
                ),
                analysis.getCompatibleItemCount(),
                compatibleItems,
                analysis.getSummary(),
                factors.explanationGenerationType(),
                analysis.getAnalyzedAt()
        );
    }

    private String findPrimaryImageUrl(Long productId) {
        return productImageRepository
                .findAllByProduct_IdInAndPrimaryTrue(
                        List.of(productId)
                )
                .stream()
                .min(
                        Comparator.comparingInt(
                                ProductImage::getSortOrder
                        )
                )
                .map(ProductImage::getUrl)
                .orElse(null);
    }
}
