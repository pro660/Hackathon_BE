package org.likelionhsu.hackathon.purchaseutility.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.preference.repository.PreferenceRepository;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.entity.ProductTagMapping;
import org.likelionhsu.hackathon.product.entity.ProductTagType;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.likelionhsu.hackathon.purchaseutility.entity.PurchaseUtilityAnalysis;
import org.likelionhsu.hackathon.purchaseutility.domain.CareDifficulty;
import org.likelionhsu.hackathon.purchaseutility.domain.CareDifficultyResolver;
import org.likelionhsu.hackathon.purchaseutility.repository.PurchaseUtilityAnalysisRepository;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityScorer.PurchaseUtilityScoreResult;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityScorer.UserItemCandidate;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseUtilityAnalysisService {

    private static final String INSUFFICIENT_DATA_MESSAGE =
            "활용 가능성을 분석하기 위한 정보가 부족해요.";

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PreferenceRepository preferenceRepository;
    private final UserItemRepository userItemRepository;
    private final UserItemImageRepository userItemImageRepository;
    private final ProductTagMappingRepository productTagMappingRepository;
    private final PurchaseUtilityAnalysisRepository analysisRepository;
    private final PurchaseUtilityScorer scorer;
    private final Clock clock;

    public PurchaseUtilityAnalysisService(
            UserRepository userRepository,
            ProductRepository productRepository,
            PreferenceRepository preferenceRepository,
            UserItemRepository userItemRepository,
            UserItemImageRepository userItemImageRepository,
            ProductTagMappingRepository productTagMappingRepository,
            PurchaseUtilityAnalysisRepository analysisRepository,
            PurchaseUtilityScorer scorer,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.preferenceRepository = preferenceRepository;
        this.userItemRepository = userItemRepository;
        this.userItemImageRepository = userItemImageRepository;
        this.productTagMappingRepository = productTagMappingRepository;
        this.analysisRepository = analysisRepository;
        this.scorer = scorer;
        this.clock = clock;
    }

    @Transactional
    public RuleAnalysisResult createRuleBasedAnalysis(
            Long userId,
            Long productId,
            Long aiJobId
    ) {
        User user = findActiveUser(userId);
        Product product = productRepository
                .findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );

        if (product.getPrimaryColor() == null) {
            return RuleAnalysisResult.insufficientData();
        }

        PreferenceProfile preference =
                preferenceRepository
                        .findByUser_Id(userId)
                        .orElse(null);

        if (preference == null) {
            return RuleAnalysisResult.insufficientData();
        }

        List<UserItem> userItems =
                findActiveUserItems(userId);

        if (userItems.isEmpty()) {
            return RuleAnalysisResult.insufficientData();
        }

        List<ProductTagMapping> tagMappings =
                productTagMappingRepository
                        .findAllWithTagByProductId(productId);

        List<String> styleTags =
                findTagCodes(
                        tagMappings,
                        ProductTagType.STYLE
                );
        List<String> seasonTags =
                findTagCodes(
                        tagMappings,
                        ProductTagType.SEASON
                );

        if (styleTags.isEmpty() || seasonTags.isEmpty()) {
            return RuleAnalysisResult.insufficientData();
        }

        List<Long> userItemIds =
                userItems
                        .stream()
                        .map(UserItem::getId)
                        .toList();

        Map<Long, String> primaryImageUrls =
                userItemImageRepository
                        .findPrimaryImageUrls(
                                userId,
                                userItemIds
                        );

        List<UserItemCandidate> candidates =
                userItems
                        .stream()
                        .map(item ->
                                new UserItemCandidate(
                                        item.getId(),
                                        item.getName(),
                                        item.getCategory(),
                                        item.getPrimaryColor(),
                                        primaryImageUrls.get(
                                                item.getId()
                                        )
                                )
                        )
                        .toList();

        PurchaseUtilityScoreResult scoreResult =
                scorer.score(
                        preference.getPreferredStyleTags(),
                        preference.getPreferredCategories(),
                        preference.getPreferredColors(),
                        product.getCategory(),
                        product.getPrimaryColor(),
                        styleTags,
                        seasonTags,
                        candidates
                );

        CareDifficulty careDifficulty =
                CareDifficultyResolver.resolve(
                        product.getMaterial()
                );

        String ruleBasedSummary =
                createRuleBasedSummary(
                        scoreResult.total(),
                        scoreResult.compatibleItemCount(),
                        careDifficulty
                );

        PurchaseUtilityAnalysis analysis =
                analysisRepository.save(
                        PurchaseUtilityAnalysis.createRuleBased(
                                user,
                                product,
                                scoreResult.total(),
                                scoreResult.compatibleItemCount(),
                                scoreResult.factors(),
                                ruleBasedSummary,
                                aiJobId,
                                Instant.now(clock)
                        )
                );

        return RuleAnalysisResult.ready(analysis);
    }

    private String createRuleBasedSummary(
            BigDecimal utilityScore,
            int compatibleItemCount,
            CareDifficulty careDifficulty
    ) {
        String utilizationSummary;

        if (utilityScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            utilizationSummary =
                    "현재 보유 아이템 및 취향과의 활용 가능성이 높은 제품입니다.";
        } else if (
                utilityScore.compareTo(BigDecimal.valueOf(60)) >= 0
        ) {
            utilizationSummary =
                    "현재 보유 아이템 및 취향과 어느 정도 활용하기 좋은 제품입니다.";
        } else {
            utilizationSummary =
                    "현재 보유 아이템 및 취향을 기준으로 활용 범위가 비교적 제한적인 제품입니다.";
        }

        String detailSummary =
                switch (careDifficulty) {
                    case EASY ->
                            "보유 아이템 중 %d개와 조합할 수 있으며, 관리 난이도는 쉬운 편입니다.";
                    case MODERATE ->
                            "보유 아이템 중 %d개와 조합할 수 있으며, 관리 난이도는 보통 수준입니다.";
                    case HARD ->
                            "보유 아이템 중 %d개와 조합할 수 있으며, 관리 난이도는 어려운 편입니다.";
                    case UNKNOWN ->
                            "보유 아이템 중 %d개와 조합할 수 있습니다.";
                };

        return utilizationSummary
                + " "
                + detailSummary.formatted(
                        compatibleItemCount
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

    private List<UserItem> findActiveUserItems(
            Long userId
    ) {
        Specification<UserItem> specification =
                UserItemSpecification
                        .ownedBy(userId)
                        .and(
                                UserItemSpecification.notDeleted()
                        );

        return userItemRepository
                .findAll(specification)
                .stream()
                .sorted(
                        Comparator.comparing(
                                UserItem::getId
                        )
                )
                .toList();
    }

    private List<String> findTagCodes(
            List<ProductTagMapping> mappings,
            ProductTagType type
    ) {
        return mappings
                .stream()
                .map(ProductTagMapping::getProductTag)
                .filter(tag -> tag.getType() == type)
                .map(tag -> tag.getCode())
                .distinct()
                .sorted()
                .toList();
    }

    public enum RuleAnalysisStatus {
        READY,
        INSUFFICIENT_DATA
    }

    public record RuleAnalysisResult(
            RuleAnalysisStatus status,
            PurchaseUtilityAnalysis analysis,
            String message
    ) {

        public static RuleAnalysisResult ready(
                PurchaseUtilityAnalysis analysis
        ) {
            return new RuleAnalysisResult(
                    RuleAnalysisStatus.READY,
                    analysis,
                    null
            );
        }

        public static RuleAnalysisResult insufficientData() {
            return new RuleAnalysisResult(
                    RuleAnalysisStatus.INSUFFICIENT_DATA,
                    null,
                    INSUFFICIENT_DATA_MESSAGE
            );
        }

        public boolean isReady() {
            return status == RuleAnalysisStatus.READY;
        }
    }
}
