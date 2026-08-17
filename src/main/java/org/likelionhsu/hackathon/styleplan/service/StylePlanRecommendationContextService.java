package org.likelionhsu.hackathon.styleplan.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.preference.repository.PreferenceRepository;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTagMapping;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanUserItemImageRepository;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StylePlanRecommendationContextService {

    private static final int MAX_OWNED_ITEMS = 10;
    private static final int MAX_PRODUCT_CANDIDATES = 10;

    private final PreferenceRepository preferenceRepository;
    private final UserItemRepository userItemRepository;
    private final StylePlanUserItemImageRepository
            userItemImageRepository;
    private final ProductRepository productRepository;
    private final ProductTagMappingRepository
            productTagMappingRepository;
    private final ProductImageRepository productImageRepository;

    public StylePlanRecommendationContextService(
            PreferenceRepository preferenceRepository,
            UserItemRepository userItemRepository,
            StylePlanUserItemImageRepository
                    userItemImageRepository,
            ProductRepository productRepository,
            ProductTagMappingRepository
                    productTagMappingRepository,
            ProductImageRepository productImageRepository
    ) {
        this.preferenceRepository = preferenceRepository;
        this.userItemRepository = userItemRepository;
        this.userItemImageRepository = userItemImageRepository;
        this.productRepository = productRepository;
        this.productTagMappingRepository =
                productTagMappingRepository;
        this.productImageRepository = productImageRepository;
    }

    @Transactional(readOnly = true)
    public StylePlanRecommendationContext prepare(
            Long userId,
            StylePlanJobRequest request
    ) {
        PreferenceSnapshot preference =
                loadPreference(userId);

        List<UserItem> activeItems =
                userItemRepository
                        .findAllByUser_IdAndDeletedAtIsNullOrderByIdAsc(
                                userId
                        );

        List<StylePlanRecommendationContext.OwnedItemCandidate>
                ownedItems =
                activeItems.stream()
                        .map(item -> toOwnedItem(
                                userId,
                                item,
                                preference
                        ))
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                StylePlanRecommendationContext
                                                        .OwnedItemCandidate
                                                        ::score
                                        )
                                        .reversed()
                                        .thenComparing(
                                                candidate ->
                                                        Long.parseLong(
                                                                candidate
                                                                        .myItemId()
                                                        )
                                        )
                        )
                        .limit(MAX_OWNED_ITEMS)
                        .toList();

        List<Product> activeMcmProducts =
                productRepository
                        .findAllByBrand(ProductBrand.MCM)
                        .stream()
                        .filter(product ->
                                product.getStatus()
                                        == ProductStatus.ACTIVE
                        )
                        .toList();

        List<Long> productIds =
                activeMcmProducts.stream()
                        .map(Product::getId)
                        .toList();

        Map<Long, List<String>> tagsByProductId =
                findTags(productIds);
        Map<Long, String> imagesByProductId =
                findProductImages(productIds);

        List<StylePlanRecommendationContext.ProductCandidate>
                productCandidates =
                activeMcmProducts.stream()
                        .map(product -> toProductCandidate(
                                product,
                                tagsByProductId.getOrDefault(
                                        product.getId(),
                                        List.of()
                                ),
                                imagesByProductId.get(
                                        product.getId()
                                ),
                                request,
                                preference
                        ))
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                StylePlanRecommendationContext
                                                        .ProductCandidate
                                                        ::score
                                        )
                                        .reversed()
                                        .thenComparing(
                                                candidate ->
                                                        Long.parseLong(
                                                                candidate
                                                                        .productId()
                                                        )
                                        )
                        )
                        .limit(MAX_PRODUCT_CANDIDATES)
                        .toList();

        return new StylePlanRecommendationContext(
                request,
                preference.styleTags(),
                preference.colors(),
                preference.categories(),
                ownedItems,
                productCandidates
        );
    }

    private PreferenceSnapshot loadPreference(
            Long userId
    ) {
        return preferenceRepository
                .findByUser_Id(userId)
                .map(this::toPreferenceSnapshot)
                .orElseGet(PreferenceSnapshot::empty);
    }

    private PreferenceSnapshot toPreferenceSnapshot(
            PreferenceProfile profile
    ) {
        return new PreferenceSnapshot(
                profile.getPreferredStyleTags()
                        .stream()
                        .map(Enum::name)
                        .sorted()
                        .toList(),
                profile.getPreferredColors()
                        .stream()
                        .map(Enum::name)
                        .sorted()
                        .toList(),
                profile.getPreferredCategories()
                        .stream()
                        .map(Enum::name)
                        .sorted()
                        .toList()
        );
    }

    private StylePlanRecommendationContext.OwnedItemCandidate
            toOwnedItem(
            Long userId,
            UserItem item,
            PreferenceSnapshot preference
    ) {
        int score = 0;

        if (preference.categories().contains(
                item.getCategory().name()
        )) {
            score += 3;
        }

        if (item.getPrimaryColor() != null
                && preference.colors().contains(
                item.getPrimaryColor().name()
        )) {
            score += 2;
        }

        String imageUrl =
                userItemImageRepository
                        .findPrimaryImageUrl(
                                userId,
                                item.getId()
                        )
                        .orElse(null);

        return new StylePlanRecommendationContext
                .OwnedItemCandidate(
                String.valueOf(item.getId()),
                item.getName(),
                imageUrl,
                item.getCategory().name(),
                enumName(item.getPrimaryColor()),
                enumName(item.getMaterial()),
                item.getVersion() == null
                        ? 0L
                        : item.getVersion(),
                score
        );
    }

    private StylePlanRecommendationContext.ProductCandidate
            toProductCandidate(
            Product product,
            List<String> tags,
            String imageUrl,
            StylePlanJobRequest request,
            PreferenceSnapshot preference
    ) {
        int score = 0;

        for (String styleTag : request.styleTags()) {
            if (tags.contains(styleTag)) {
                score += 5;
            }
        }

        if (tags.contains(request.occasion())) {
            score += 4;
        }

        for (String styleTag : preference.styleTags()) {
            if (tags.contains(styleTag)) {
                score += 2;
            }
        }

        if (preference.categories().contains(
                product.getCategory().name()
        )) {
            score += 2;
        }

        if (product.getPrimaryColor() != null
                && preference.colors().contains(
                product.getPrimaryColor().name()
        )) {
            score += 1;
        }

        return new StylePlanRecommendationContext
                .ProductCandidate(
                String.valueOf(product.getId()),
                product.getName(),
                imageUrl,
                product.getCategory().name(),
                enumName(product.getPrimaryColor()),
                enumName(product.getMaterial()),
                tags,
                score
        );
    }

    private Map<Long, List<String>> findTags(
            List<Long> productIds
    ) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Set<String>> grouped =
                productTagMappingRepository
                        .findAllWithTagByProductIdIn(
                                productIds
                        )
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        mapping ->
                                                mapping.getProduct()
                                                        .getId(),
                                        HashMap::new,
                                        Collectors.mapping(
                                                mapping ->
                                                        mapping
                                                                .getProductTag()
                                                                .getCode(),
                                                Collectors.toSet()
                                        )
                                )
                        );

        Map<Long, List<String>> result =
                new HashMap<>();

        grouped.forEach((productId, tags) ->
                result.put(
                        productId,
                        tags.stream()
                                .sorted()
                                .toList()
                )
        );

        return result;
    }

    private Map<Long, String> findProductImages(
            List<Long> productIds
    ) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<ProductImage> images =
                new ArrayList<>(
                        productImageRepository
                                .findAllByProduct_IdInAndPrimaryTrue(
                                        productIds
                                )
                );

        images.sort(
                Comparator.comparing(
                        ProductImage::getId
                )
        );

        Map<Long, String> result =
                new HashMap<>();

        for (ProductImage image : images) {
            result.putIfAbsent(
                    image.getProduct().getId(),
                    image.getUrl()
            );
        }

        return result;
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private record PreferenceSnapshot(
            List<String> styleTags,
            List<String> colors,
            List<String> categories
    ) {

        private PreferenceSnapshot {
            styleTags = List.copyOf(styleTags);
            colors = List.copyOf(colors);
            categories = List.copyOf(categories);
        }

        private static PreferenceSnapshot empty() {
            return new PreferenceSnapshot(
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
    }
}
