package org.likelionhsu.hackathon.styleplan.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.likelionhsu.hackathon.styleplan.ai.StylePlanAiSelection;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationException;
import org.likelionhsu.hackathon.styleplan.ai.StylePlanGenerationException.FailureKind;
import org.likelionhsu.hackathon.styleplan.dto.StylePlanPreview;
import org.springframework.stereotype.Component;

@Component
public class StylePlanPreviewAssembler {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 1500;
    private static final int MAX_REASON_LENGTH = 1000;
    private static final int MAX_OWNED_ITEMS = 10;
    private static final int MAX_RECOMMENDED_PRODUCTS = 3;

    private static final Set<String> ALLOWED_ROLES =
            Set.of(
                    "MAIN",
                    "TOP",
                    "BOTTOM",
                    "SHOES",
                    "BAG",
                    "ACCESSORY"
            );

    public StylePlanPreview assemble(
            Long jobId,
            StylePlanRecommendationContext context,
            StylePlanAiSelection selection
    ) {
        requireText(
                selection.title(),
                "title",
                MAX_TITLE_LENGTH
        );
        requireText(
                selection.description(),
                "description",
                MAX_DESCRIPTION_LENGTH
        );

        if (selection.ownedItems().size()
                > MAX_OWNED_ITEMS) {
            throw invalid(
                    "AI가 보유 아이템을 10개 초과 선택했습니다."
            );
        }

        if (selection.recommendedProducts().size()
                > MAX_RECOMMENDED_PRODUCTS) {
            throw invalid(
                    "AI가 MCM 상품을 3개 초과 선택했습니다."
            );
        }

        Map<String, StylePlanRecommendationContext
                .OwnedItemCandidate> ownedById =
                new HashMap<>();

        for (var candidate : context.ownedItems()) {
            ownedById.put(
                    candidate.myItemId(),
                    candidate
            );
        }

        Map<String, StylePlanRecommendationContext
                .ProductCandidate> productById =
                new HashMap<>();

        for (var candidate :
                context.productCandidates()) {
            productById.put(
                    candidate.productId(),
                    candidate
            );
        }

        List<StylePlanPreview.OwnedItem> ownedItems =
                assembleOwnedItems(
                        selection,
                        ownedById
                );

        List<StylePlanPreview.RecommendedProduct>
                recommendedProducts =
                assembleProducts(
                        selection,
                        productById
                );

        return new StylePlanPreview(
                "job:" + jobId,
                selection.title().trim(),
                selection.description().trim(),
                ownedItems,
                recommendedProducts,
                "AI"
        );
    }

    private List<StylePlanPreview.OwnedItem>
            assembleOwnedItems(
            StylePlanAiSelection selection,
            Map<String, StylePlanRecommendationContext
                    .OwnedItemCandidate> ownedById
    ) {
        List<StylePlanPreview.OwnedItem> result =
                new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int index = 0;
             index < selection.ownedItems().size();
             index++) {
            StylePlanAiSelection.OwnedItemSelection
                    selected =
                    selection.ownedItems().get(index);

            requireText(
                    selected.myItemId(),
                    "ownedItems.myItemId",
                    50
            );
            requireText(
                    selected.role(),
                    "ownedItems.role",
                    30
            );

            if (!seen.add(selected.myItemId())) {
                throw invalid(
                        "AI가 같은 보유 아이템을 중복 선택했습니다."
                );
            }

            if (!ALLOWED_ROLES.contains(
                    selected.role()
            )) {
                throw invalid(
                        "AI가 허용되지 않은 role을 반환했습니다."
                );
            }

            var candidate =
                    ownedById.get(
                            selected.myItemId()
                    );

            if (candidate == null) {
                throw invalid(
                        "AI가 서버 후보에 없는 보유 아이템을 선택했습니다."
                );
            }

            result.add(
                    new StylePlanPreview.OwnedItem(
                            candidate.myItemId(),
                            candidate.name(),
                            candidate.imageUrl(),
                            selected.role(),
                            index
                    )
            );
        }

        return List.copyOf(result);
    }

    private List<StylePlanPreview.RecommendedProduct>
            assembleProducts(
            StylePlanAiSelection selection,
            Map<String, StylePlanRecommendationContext
                    .ProductCandidate> productById
    ) {
        List<StylePlanPreview.RecommendedProduct>
                result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int index = 0;
             index < selection
                     .recommendedProducts()
                     .size();
             index++) {
            StylePlanAiSelection.ProductSelection
                    selected =
                    selection.recommendedProducts()
                            .get(index);

            requireText(
                    selected.productId(),
                    "recommendedProducts.productId",
                    50
            );
            requireText(
                    selected.reason(),
                    "recommendedProducts.reason",
                    MAX_REASON_LENGTH
            );

            if (!seen.add(selected.productId())) {
                throw invalid(
                        "AI가 같은 MCM 상품을 중복 선택했습니다."
                );
            }

            var candidate =
                    productById.get(
                            selected.productId()
                    );

            if (candidate == null) {
                throw invalid(
                        "AI가 서버 후보에 없는 MCM 상품을 선택했습니다."
                );
            }

            result.add(
                    new StylePlanPreview
                            .RecommendedProduct(
                            candidate.productId(),
                            candidate.name(),
                            candidate.imageUrl(),
                            index + 1,
                            selected.reason().trim()
                    )
            );
        }

        return List.copyOf(result);
    }

    private void requireText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null
                || value.isBlank()
                || value.length() > maxLength) {
            throw invalid(
                    field
                            + "가 길이 규칙을 만족하지 않습니다."
            );
        }
    }

    private StylePlanGenerationException invalid(
            String message
    ) {
        return new StylePlanGenerationException(
                FailureKind.INVALID_RESPONSE,
                message
        );
    }
}
