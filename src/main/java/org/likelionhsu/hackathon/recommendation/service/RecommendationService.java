package org.likelionhsu.hackathon.recommendation.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.preference.repository.PreferenceRepository;
import org.likelionhsu.hackathon.product.dto.response.ProductTagsResponse;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductImage;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTagMapping;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductSpecification;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.likelionhsu.hackathon.recommendation.dto.request.RecommendationRequest;
import org.likelionhsu.hackathon.recommendation.dto.response.RecommendationProductResponse;
import org.likelionhsu.hackathon.recommendation.dto.response.RecommendationResponse;
import org.likelionhsu.hackathon.recommendation.dto.response.RecommendationScoreBreakdownResponse;
import org.likelionhsu.hackathon.recommendation.entity.Recommendation;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationProduct;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationContextSnapshot;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationProductSnapshot;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationProductTagsSnapshot;
import org.likelionhsu.hackathon.recommendation.entity.snapshot.RecommendationScoreBreakdownSnapshot;
import org.likelionhsu.hackathon.recommendation.repository.RecommendationProductRepository;
import org.likelionhsu.hackathon.recommendation.repository.RecommendationRepository;
import org.likelionhsu.hackathon.recommendation.service.RecommendationScorer.RecommendationScoreResult;
import org.likelionhsu.hackathon.recommendation.validation.RecommendationRequestValidator;
import org.likelionhsu.hackathon.recommendation.validation.RecommendationRequestValidator.ValidatedRecommendationRequest;
import org.likelionhsu.hackathon.wishlist.repository.WishlistRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    public static final String SCORE_POLICY_VERSION =
            "product-recommendation-v1";

    private final UserRepository userRepository;
    private final PreferenceRepository preferenceRepository;
    private final ProductRepository productRepository;
    private final ProductTagMappingRepository productTagMappingRepository;
    private final ProductImageRepository productImageRepository;
    private final WishlistRepository wishlistRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationProductRepository recommendationProductRepository;
    private final RecommendationRequestValidator requestValidator;
    private final RecommendationScorer scorer;
    private final RecommendationReasonBuilder reasonBuilder;
    private final Clock clock;

    public RecommendationService(
            UserRepository userRepository,
            PreferenceRepository preferenceRepository,
            ProductRepository productRepository,
            ProductTagMappingRepository productTagMappingRepository,
            ProductImageRepository productImageRepository,
            WishlistRepository wishlistRepository,
            RecommendationRepository recommendationRepository,
            RecommendationProductRepository recommendationProductRepository,
            RecommendationRequestValidator requestValidator,
            RecommendationScorer scorer,
            RecommendationReasonBuilder reasonBuilder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.productRepository = productRepository;
        this.productTagMappingRepository = productTagMappingRepository;
        this.productImageRepository = productImageRepository;
        this.wishlistRepository = wishlistRepository;
        this.recommendationRepository = recommendationRepository;
        this.recommendationProductRepository = recommendationProductRepository;
        this.requestValidator = requestValidator;
        this.scorer = scorer;
        this.reasonBuilder = reasonBuilder;
        this.clock = clock;
    }

    @Transactional
    public RecommendationResponse createRecommendation(
            Long userId,
            RecommendationRequest request
    ) {
        ValidatedRecommendationRequest validatedRequest =
                requestValidator.validate(request);
        User user = findActiveUser(userId);
        PreferenceProfile preference =
                preferenceRepository
                        .findByUser_Id(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.PREFERENCE_REQUIRED
                                )
                        );

        List<String> preferredStyleTags =
                preference.getPreferredStyleTags()
                        .stream()
                        .map(Enum::name)
                        .sorted()
                        .toList();

        List<Product> candidates =
                findCandidates(validatedRequest);
        Map<Long, RecommendationProductTagsSnapshot> tagsByProductId =
                findProductTags(candidates);
        Map<Long, String> primaryImageUrls =
                findPrimaryImageUrls(candidates);

        List<ScoredProduct> scoredProducts =
                candidates
                        .stream()
                        .map(product -> scoreProduct(
                                product,
                                preferredStyleTags,
                                validatedRequest,
                                tagsByProductId.getOrDefault(
                                        product.getId(),
                                        RecommendationProductTagsSnapshot.empty()
                                ),
                                primaryImageUrls.get(product.getId())
                        ))
                        .filter(scored ->
                                scored.scoreResult().hasScore()
                        )
                        .sorted(
                                Comparator
                                        .comparing(
                                                (ScoredProduct scored) ->
                                                        scored.scoreResult().total()
                                        )
                                        .reversed()
                                        .thenComparing(scored ->
                                                scored.product().getId()
                                        )
                        )
                        .limit(validatedRequest.limit())
                        .toList();

        String summary =
                reasonBuilder.buildSummary(
                        validatedRequest,
                        scoredProducts.size()
                );
        Instant generatedAt = Instant.now(clock);

        RecommendationContextSnapshot context =
                new RecommendationContextSnapshot(
                        SCORE_POLICY_VERSION,
                        preferredStyleTags,
                        validatedRequest.occasion(),
                        validatedRequest.season(),
                        validatedRequest.preferredFeatures(),
                        validatedRequest.category(),
                        validatedRequest.limit()
                );

        Recommendation recommendation =
                recommendationRepository.save(
                        Recommendation.createRuleBased(
                                user,
                                summary,
                                context,
                                generatedAt
                        )
                );

        List<RecommendationProduct> recommendationProducts =
                new ArrayList<>();

        for (int index = 0;
             index < scoredProducts.size();
             index++) {
            ScoredProduct scored = scoredProducts.get(index);

            recommendationProducts.add(
                    RecommendationProduct.create(
                            recommendation,
                            scored.product(),
                            index + 1,
                            scored.scoreResult().total(),
                            scored.reason(),
                            scored.snapshot()
                    )
            );
        }

        recommendationProductRepository.saveAll(
                recommendationProducts
        );

        Set<Long> favoritedProductIds =
                findFavoritedProductIds(
                        userId,
                        scoredProducts
                                .stream()
                                .map(ScoredProduct::product)
                                .toList()
                );

        List<RecommendationProductResponse> products =
                scoredProducts
                        .stream()
                        .map(scored -> toProductResponse(
                                scored.snapshot(),
                                scored.scoreResult().total(),
                                scored.reason(),
                                favoritedProductIds.contains(
                                        scored.product().getId()
                                )
                        ))
                        .toList();

        return new RecommendationResponse(
                String.valueOf(recommendation.getId()),
                recommendation.getGenerationType(),
                SCORE_POLICY_VERSION,
                recommendation.getSummary(),
                products,
                recommendation.getGeneratedAt()
        );
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendation(
            Long userId,
            Long recommendationId
    ) {
        findActiveUser(userId);

        Recommendation recommendation =
                recommendationRepository
                        .findByIdAndUser_Id(
                                recommendationId,
                                userId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.RECOMMENDATION_NOT_FOUND
                                )
                        );

        List<RecommendationProduct> recommendationProducts =
                recommendationProductRepository
                        .findAllWithProductByRecommendationId(
                                recommendationId
                        );

        Set<Long> favoritedProductIds =
                findFavoritedProductIds(
                        userId,
                        recommendationProducts
                                .stream()
                                .map(RecommendationProduct::getProduct)
                                .toList()
                );

        List<RecommendationProductResponse> products =
                recommendationProducts
                        .stream()
                        .map(recommendationProduct ->
                                toProductResponse(
                                        recommendationProduct
                                                .getProductSnapshot(),
                                        recommendationProduct.getScore(),
                                        recommendationProduct.getReason(),
                                        favoritedProductIds.contains(
                                                recommendationProduct
                                                        .getProduct()
                                                        .getId()
                                        )
                                )
                        )
                        .toList();

        return new RecommendationResponse(
                String.valueOf(recommendation.getId()),
                recommendation.getGenerationType(),
                recommendation
                        .getContextJson()
                        .scorePolicyVersion(),
                recommendation.getSummary(),
                products,
                recommendation.getGeneratedAt()
        );
    }

    private User findActiveUser(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.ACCESS_TOKEN_INVALID
                                )
                        );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_NOT_ACTIVE
            );
        }

        return user;
    }

    private List<Product> findCandidates(
            ValidatedRecommendationRequest request
    ) {
        Specification<Product> specification =
                ProductSpecification
                        .hasStatus(ProductStatus.ACTIVE)
                        .and(
                                ProductSpecification.hasBrand(
                                        ProductBrand.MCM
                                )
                        )
                        .and(
                                ProductSpecification.hasCategory(
                                        request.category()
                                )
                        );

        return productRepository.findAll(specification);
    }

    private ScoredProduct scoreProduct(
            Product product,
            List<String> preferredStyleTags,
            ValidatedRecommendationRequest request,
            RecommendationProductTagsSnapshot tags,
            String primaryImageUrl
    ) {
        RecommendationScoreResult scoreResult =
                scorer.score(
                        preferredStyleTags,
                        request.occasion(),
                        request.season(),
                        request.preferredFeatures(),
                        tags
                );

        String reason =
                scoreResult.hasScore()
                        ? reasonBuilder.buildReason(
                                scoreResult,
                                request.occasion(),
                                request.season()
                        )
                        : null;

        RecommendationProductSnapshot snapshot =
                new RecommendationProductSnapshot(
                        String.valueOf(product.getId()),
                        product.getName(),
                        product.getCategory(),
                        product.getPrice(),
                        product.getPrimaryColor(),
                        primaryImageUrl,
                        tags,
                        scoreResult.breakdown()
                );

        return new ScoredProduct(
                product,
                scoreResult,
                reason,
                snapshot
        );
    }

    private Map<Long, RecommendationProductTagsSnapshot> findProductTags(
            List<Product> products
    ) {
        if (products.isEmpty()) {
            return Map.of();
        }

        List<Long> productIds =
                products
                        .stream()
                        .map(Product::getId)
                        .toList();

        List<ProductTagMapping> mappings =
                productTagMappingRepository
                        .findAllWithTagByProductIdIn(productIds);

        Map<Long, EnumMap<ProductTagType, List<String>>> grouped =
                new HashMap<>();

        for (ProductTagMapping mapping : mappings) {
            Long productId = mapping.getProduct().getId();
            EnumMap<ProductTagType, List<String>> byType =
                    grouped.computeIfAbsent(
                            productId,
                            ignored -> newTagMap()
                    );

            byType
                    .get(mapping.getProductTag().getType())
                    .add(mapping.getProductTag().getCode());
        }

        Map<Long, RecommendationProductTagsSnapshot> result =
                new HashMap<>();

        for (Product product : products) {
            EnumMap<ProductTagType, List<String>> byType =
                    grouped.getOrDefault(
                            product.getId(),
                            newTagMap()
                    );

            result.put(
                    product.getId(),
                    new RecommendationProductTagsSnapshot(
                            sorted(byType.get(ProductTagType.STYLE)),
                            sorted(byType.get(ProductTagType.SEASON)),
                            sorted(byType.get(ProductTagType.OCCASION)),
                            sorted(byType.get(ProductTagType.FEATURE))
                    )
            );
        }

        return Map.copyOf(result);
    }

    private EnumMap<ProductTagType, List<String>> newTagMap() {
        EnumMap<ProductTagType, List<String>> map =
                new EnumMap<>(ProductTagType.class);

        for (ProductTagType type : ProductTagType.values()) {
            map.put(type, new ArrayList<>());
        }

        return map;
    }

    private List<String> sorted(List<String> values) {
        return values
                .stream()
                .sorted()
                .toList();
    }

    private Map<Long, String> findPrimaryImageUrls(
            List<Product> products
    ) {
        if (products.isEmpty()) {
            return Map.of();
        }

        List<Long> productIds =
                products
                        .stream()
                        .map(Product::getId)
                        .toList();

        return productImageRepository
                .findAllByProduct_IdInAndPrimaryTrue(productIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                image -> image.getProduct().getId(),
                                ProductImage::getUrl,
                                (first, second) -> first
                        )
                );
    }

    private Set<Long> findFavoritedProductIds(
            Long userId,
            List<Product> products
    ) {
        if (products.isEmpty()) {
            return Set.of();
        }

        List<Long> productIds =
                products
                        .stream()
                        .map(Product::getId)
                        .toList();

        return wishlistRepository
                .findProductIdsByUserIdAndProductIdIn(
                        userId,
                        productIds
                );
    }

    private RecommendationProductResponse toProductResponse(
            RecommendationProductSnapshot snapshot,
            BigDecimal score,
            String reason,
            boolean favorited
    ) {
        RecommendationProductTagsSnapshot tags = snapshot.tags();
        RecommendationScoreBreakdownSnapshot breakdown =
                snapshot.scoreBreakdown();

        return new RecommendationProductResponse(
                snapshot.productId(),
                snapshot.name(),
                snapshot.category(),
                snapshot.price(),
                snapshot.primaryColor(),
                snapshot.primaryImageUrl(),
                new ProductTagsResponse(
                        tags.styles(),
                        tags.seasons(),
                        tags.occasions(),
                        tags.features()
                ),
                score,
                new RecommendationScoreBreakdownResponse(
                        breakdown.style(),
                        breakdown.occasion(),
                        breakdown.season(),
                        breakdown.feature()
                ),
                reason,
                favorited
        );
    }

    private record ScoredProduct(
            Product product,
            RecommendationScoreResult scoreResult,
            String reason,
            RecommendationProductSnapshot snapshot
    ) {
    }
}
