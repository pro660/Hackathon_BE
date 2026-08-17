package org.likelionhsu.hackathon.styleplan.service;

import java.util.List;

public record StylePlanRecommendationContext(
        StylePlanJobRequest request,
        List<String> preferredStyleTags,
        List<String> preferredColors,
        List<String> preferredCategories,
        List<OwnedItemCandidate> ownedItems,
        List<ProductCandidate> productCandidates
) {

    public StylePlanRecommendationContext {
        preferredStyleTags = List.copyOf(preferredStyleTags);
        preferredColors = List.copyOf(preferredColors);
        preferredCategories = List.copyOf(preferredCategories);
        ownedItems = List.copyOf(ownedItems);
        productCandidates = List.copyOf(productCandidates);
    }

    public record OwnedItemCandidate(
            String myItemId,
            String name,
            String imageUrl,
            String category,
            String primaryColor,
            String material,
            long version,
            int score
    ) {
    }

    public record ProductCandidate(
            String productId,
            String name,
            String imageUrl,
            String category,
            String primaryColor,
            String material,
            List<String> tags,
            int score
    ) {

        public ProductCandidate {
            tags = List.copyOf(tags);
        }
    }
}
