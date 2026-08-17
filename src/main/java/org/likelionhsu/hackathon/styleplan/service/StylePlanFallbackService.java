package org.likelionhsu.hackathon.styleplan.service;

import java.util.ArrayList;
import java.util.List;

import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;
import org.springframework.stereotype.Service;

@Service
public class StylePlanFallbackService {

    private static final int MAX_RECOMMENDED_PRODUCTS = 3;

    public StylePlanPreview build(
            Long jobId,
            StylePlanRecommendationContext context
    ) {
        List<StylePlanPreview.OwnedItem> ownedItems =
                toOwnedItems(context);

        List<StylePlanPreview.RecommendedProduct>
                recommendedProducts =
                toRecommendedProducts(context);

        return new StylePlanPreview(
                "job:" + jobId,
                titleFor(context.request().occasion()),
                descriptionFor(
                        context,
                        ownedItems.size(),
                        recommendedProducts.size()
                ),
                ownedItems,
                recommendedProducts,
                "RULE_BASED"
        );
    }

    private List<StylePlanPreview.OwnedItem>
            toOwnedItems(
            StylePlanRecommendationContext context
    ) {
        List<StylePlanPreview.OwnedItem> result =
                new ArrayList<>();

        for (int index = 0;
             index < context.ownedItems().size();
             index++) {
            var item =
                    context.ownedItems().get(index);

            result.add(
                    new StylePlanPreview.OwnedItem(
                            item.myItemId(),
                            item.name(),
                            item.imageUrl(),
                            roleFor(item.category()),
                            index
                    )
            );
        }

        return List.copyOf(result);
    }

    private String roleFor(String category) {
        return switch (category) {
            case "BAG" -> "BAG";
            case "SHOES" -> "SHOES";
            case "LEATHER_GOODS",
                 "FASHION_ACCESSORY" -> "ACCESSORY";
            case "CLOTHING" -> "MAIN";
            default -> "MAIN";
        };
    }

    private List<StylePlanPreview.RecommendedProduct>
            toRecommendedProducts(
            StylePlanRecommendationContext context
    ) {
        List<StylePlanPreview.RecommendedProduct>
                result = new ArrayList<>();

        int count = Math.min(
                MAX_RECOMMENDED_PRODUCTS,
                context.productCandidates().size()
        );

        for (int index = 0;
             index < count;
             index++) {
            var product =
                    context.productCandidates().get(index);

            result.add(
                    new StylePlanPreview.RecommendedProduct(
                            product.productId(),
                            product.name(),
                            product.imageUrl(),
                            index + 1,
                            reasonFor(
                                    product,
                                    context.request()
                            )
                    )
            );
        }

        return List.copyOf(result);
    }

    private String reasonFor(
            StylePlanRecommendationContext.ProductCandidate
                    product,
            StylePlanJobRequest request
    ) {
        boolean styleMatch =
                request.styleTags()
                        .stream()
                        .anyMatch(
                                product.tags()::contains
                        );

        boolean occasionMatch =
                product.tags().contains(
                        request.occasion()
                );

        if (styleMatch && occasionMatch) {
            return "요청한 분위기와 상황 조건에 모두 잘 맞는 상품이에요.";
        }

        if (styleMatch) {
            return "요청한 분위기와 잘 맞는 상품이에요.";
        }

        if (occasionMatch) {
            return "선택한 상황에 활용하기 좋은 상품이에요.";
        }

        return "취향과 현재 보유 아이템 정보를 기준으로 고른 상품이에요.";
    }

    private String titleFor(String occasion) {
        return switch (occasion) {
            case "DATE" -> "데이트 룩";
            case "TRAVEL" -> "여행 룩";
            case "GATHERING" -> "모임 룩";
            case "CEREMONY" -> "격식 있는 룩";
            case "OUTDOOR" -> "아웃도어 룩";
            case "DAILY" -> "데일리 룩";
            default -> "오늘의 추천 룩";
        };
    }

    private String descriptionFor(
            StylePlanRecommendationContext context,
            int itemCount,
            int productCount
    ) {
        String styles = String.join(
                ", ",
                context.request().styleTags()
        );

        if (itemCount == 0) {
            return styles
                    + " 분위기를 기준으로 MCM 상품 "
                    + productCount
                    + "개를 골랐어요.";
        }

        return styles
                + " 분위기와 보유 아이템 "
                + itemCount
                + "개를 중심으로 MCM 상품 "
                + productCount
                + "개를 함께 골랐어요.";
    }
}
