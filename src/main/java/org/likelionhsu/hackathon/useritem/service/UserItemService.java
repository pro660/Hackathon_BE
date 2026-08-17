package org.likelionhsu.hackathon.useritem.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemCreateRequest;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemUpdateRequest;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemCreateResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemDetailResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemImageResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemListItemResponse;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemAiJobValidator;
import org.likelionhsu.hackathon.useritem.repository.UserItemAiJobValidator.ItemAnalysisProvenance;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageData;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemSpecification;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;

@Service
@Transactional(readOnly = true)
public class UserItemService {


    private final UserItemRepository userItemRepository;
    private final UserItemImageRepository userItemImageRepository;
    private final UserItemAiJobValidator userItemAiJobValidator;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    public UserItemService(
            UserItemRepository userItemRepository,
            UserItemImageRepository userItemImageRepository,
            UserItemAiJobValidator userItemAiJobValidator,
            UserRepository userRepository,
            ProductRepository productRepository,
            Clock clock
    ) {
        this.userItemRepository = userItemRepository;
        this.userItemImageRepository = userItemImageRepository;
        this.userItemAiJobValidator = userItemAiJobValidator;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

    public PageResponse<UserItemListItemResponse> getMyItems(
            Long userId,
            String keyword,
            ItemCategory category,
            ColorGroup color,
            Pageable pageable
    ) {
        Specification<UserItem> specification =
                UserItemSpecification.ownedBy(userId)
                        .and(UserItemSpecification.notDeleted())
                        .and(
                                UserItemSpecification.containsKeyword(
                                        keyword
                                )
                        )
                        .and(
                                UserItemSpecification.hasCategory(
                                        category
                                )
                        )
                        .and(
                                UserItemSpecification.hasPrimaryColor(
                                        color
                                )
                        );

        Page<UserItem> itemPage = userItemRepository.findAll(
                specification,
                pageable
        );

        List<Long> itemIds = itemPage.getContent()
                .stream()
                .map(UserItem::getId)
                .toList();

        Map<Long, String> primaryImageUrls =
                userItemImageRepository.findPrimaryImageUrls(
                        userId,
                        itemIds
                );

        Page<UserItemListItemResponse> responsePage =
                itemPage.map(item ->
                        new UserItemListItemResponse(
                                String.valueOf(item.getId()),
                                item.getName(),
                                item.getBrandName(),
                                item.getCategory(),
                                item.getPrimaryColor(),
                                item.getMaterial(),
                                primaryImageUrls.get(item.getId()),
                                item.getCreatedAt()
                        )
                );

        return PageResponse.from(responsePage);
    }

    public UserItemDetailResponse getMyItem(
            Long userId,
            Long myItemId
    ) {
        return toDetailResponse(
                userId,
                findOwnedActiveItem(
                        userId,
                        myItemId
                )
        );
    }

    @Transactional
    public UserItemCreateResponse createMyItem(
            Long userId,
            UserItemCreateRequest request
    ) {
        Product product = findLinkedProduct(
                request.productId()
        );

        ItemAnalysisProvenance analysisProvenance =
                request.aiJobId() == null
                        ? null
                        : userItemAiJobValidator
                                .validateOwnedSucceededItemAnalysis(
                                        userId,
                                        request.aiJobId()
                                );

        MaterialSource materialSource = resolveMaterialSource(
                request.material(),
                request.materialSource(),
                product,
                analysisProvenance
        );

        User user = userRepository.getReferenceById(userId);

        UserItem userItem = UserItem.create(
                user,
                product,
                normalizeBrandName(request.brandName()),
                normalizeRequiredText(
                        "name",
                        request.name(),
                        200
                ),
                request.category(),
                request.primaryColor(),
                request.material(),
                materialSource,
                request.purchaseDate(),
                request.purchasePrice(),
                normalizeOptionalText(
                        "memo",
                        request.memo(),
                        1000
                ),
                request.aiJobId(),
                request.nextCareDate()
        );

        UserItem saved = userItemRepository.save(userItem);

        return new UserItemCreateResponse(
                String.valueOf(saved.getId())
        );
    }

    @Transactional
    public UserItemDetailResponse updateMyItem(
            Long userId,
            Long myItemId,
            UserItemUpdateRequest request
    ) {
        if (!request.hasChanges()) {
            throw new RequestValidationException(
                    "request",
                    "수정할 필드를 하나 이상 입력해 주세요."
            );
        }

        if (request.isAiJobIdPresent()) {
            throw new RequestValidationException(
                    "aiJobId",
                    "마이 아이템 생성 후에는 변경할 수 없습니다."
            );
        }

        UserItem item = findOwnedActiveItem(
                userId,
                myItemId
        );

        if (!request.getVersion().equals(item.getVersion())) {
            throw versionConflict();
        }

        Product product = request.isProductIdPresent()
                ? findLinkedProduct(request.getProductId())
                : item.getProduct();

        String brandName = request.isBrandNamePresent()
                ? normalizeBrandName(request.getBrandName())
                : item.getBrandName();

        String name = request.isNamePresent()
                ? normalizeRequiredText(
                        "name",
                        request.getName(),
                        200
                )
                : item.getName();

        ItemCategory category = request.isCategoryPresent()
                ? requireCategory(request.getCategory())
                : item.getCategory();

        ColorGroup primaryColor = request.isPrimaryColorPresent()
                ? request.getPrimaryColor()
                : item.getPrimaryColor();

        MaterialGroup material = request.isMaterialPresent()
                ? request.getMaterial()
                : item.getMaterial();

        Long aiJobId = item.getAiJobId();

        MaterialSource materialSource =
                resolveUpdatedMaterialSource(
                        userId,
                        request,
                        item,
                        material,
                        product,
                        aiJobId
                );

        String memo = request.isMemoPresent()
                ? normalizeOptionalText(
                        "memo",
                        request.getMemo(),
                        1000
                )
                : item.getMemo();

        item.update(
                product,
                brandName,
                name,
                category,
                primaryColor,
                material,
                materialSource,
                request.isPurchaseDatePresent()
                        ? request.getPurchaseDate()
                        : item.getPurchaseDate(),
                request.isPurchasePricePresent()
                        ? request.getPurchasePrice()
                        : item.getPurchasePrice(),
                memo,
                request.isNextCareDatePresent()
                        ? request.getNextCareDate()
                        : item.getNextCareDate()
        );

        flushWithVersionConflict(item);

        return toDetailResponse(userId, item);
    }

    @Transactional
    public void deleteMyItem(
            Long userId,
            Long myItemId
    ) {
        UserItem item = userItemRepository
                .findByIdAndUser_Id(
                        myItemId,
                        userId
                )
                .orElseThrow(this::myItemNotFound);

        if (item.getDeletedAt() != null) {
            return;
        }

        userItemImageRepository.markDeletePending(
                userId,
                myItemId
        );

        item.softDelete(clock.instant());
        flushWithVersionConflict(item);
    }

    private UserItem findOwnedActiveItem(
            Long userId,
            Long myItemId
    ) {
        return userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        myItemId,
                        userId
                )
                .orElseThrow(this::myItemNotFound);
    }

    private Product findLinkedProduct(Long productId) {
        if (productId == null) {
            return null;
        }

        return productRepository
                .findByIdAndStatus(
                        productId,
                        ProductStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );
    }

    private MaterialSource resolveUpdatedMaterialSource(
            Long userId,
            UserItemUpdateRequest request,
            UserItem item,
            MaterialGroup material,
            Product product,
            Long aiJobId
    ) {
        if (material == null) {
            if (request.isMaterialSourcePresent()
                    && request.getMaterialSource() != null) {
                throw new RequestValidationException(
                        "materialSource",
                        "소재가 없으면 소재 출처를 입력할 수 없습니다."
                );
            }

            return null;
        }

        MaterialSource source;

        if (request.isMaterialSourcePresent()) {
            source = request.getMaterialSource();
        } else if (request.isMaterialPresent()
                && !Objects.equals(
                        item.getMaterial(),
                        material
                )) {
            source = MaterialSource.USER_CONFIRMED;
        } else if (request.isProductIdPresent()
                && item.getMaterialSource()
                == MaterialSource.PRODUCT_DATA
                && !Objects.equals(
                        item.getProduct() == null
                                ? null
                                : item.getProduct().getId(),
                        product == null
                                ? null
                                : product.getId()
                )) {
            source = MaterialSource.USER_CONFIRMED;
        } else {
            source = item.getMaterialSource();
        }

        ItemAnalysisProvenance analysisProvenance = null;

        if (source == MaterialSource.AI_ESTIMATED
                && aiJobId != null) {
            analysisProvenance =
                    userItemAiJobValidator
                            .validateOwnedSucceededItemAnalysis(
                                    userId,
                                    aiJobId
                            );
        }

        return resolveMaterialSource(
                material,
                source,
                product,
                analysisProvenance
        );
    }

    private MaterialSource resolveMaterialSource(
            MaterialGroup material,
            MaterialSource materialSource,
            Product product,
            ItemAnalysisProvenance analysisProvenance
    ) {
        if (material == null) {
            if (materialSource != null) {
                throw new RequestValidationException(
                        "materialSource",
                        "소재가 없으면 소재 출처를 입력할 수 없습니다."
                );
            }

            return null;
        }

        MaterialSource resolved = materialSource == null
                ? MaterialSource.USER_CONFIRMED
                : materialSource;

        if (resolved == MaterialSource.PRODUCT_DATA) {
            if (product == null) {
                throw new RequestValidationException(
                        "materialSource",
                        "PRODUCT_DATA는 연결된 제품이 필요합니다."
                );
            }

            if (!Objects.equals(
                    material,
                    product.getMaterial()
            )) {
                throw new RequestValidationException(
                        "material",
                        "연결된 제품의 소재와 일치해야 합니다."
                );
            }
        }

        if (resolved == MaterialSource.AI_ESTIMATED) {
            if (analysisProvenance == null) {
                throw new RequestValidationException(
                        "materialSource",
                        "AI_ESTIMATED는 아이템 분석 작업이 필요합니다."
                );
            }

            if (!Objects.equals(
                    material,
                    analysisProvenance.result().material()
            )) {
                throw new RequestValidationException(
                        "material",
                        "아이템 분석 결과의 소재와 일치해야 합니다."
                );
            }
        }

        return resolved;
    }

    private String normalizeBrandName(String brandName) {
        return normalizeOptionalText(
                "brandName",
                brandName,
                100
        );
    }

    private String normalizeRequiredText(
            String field,
            String value,
            int maxLength
    ) {
        if (value == null) {
            throw new RequestValidationException(
                    field,
                    "필수 입력값입니다."
            );
        }

        String normalized = value.trim();

        if (normalized.isEmpty()
                || normalized.length() > maxLength) {
            throw new RequestValidationException(
                    field,
                    "1~" + maxLength + "자여야 합니다."
            );
        }

        return normalized;
    }

    private String normalizeOptionalText(
            String field,
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.isEmpty()
                || normalized.length() > maxLength) {
            throw new RequestValidationException(
                    field,
                    "1~" + maxLength + "자여야 합니다."
            );
        }

        return normalized;
    }

    private ItemCategory requireCategory(
            ItemCategory category
    ) {
        if (category == null) {
            throw new RequestValidationException(
                    "category",
                    "필수 입력값입니다."
            );
        }

        return category;
    }

    private void flushWithVersionConflict(UserItem item) {
        try {
            userItemRepository.saveAndFlush(item);
        } catch (OptimisticLockingFailureException
                 | OptimisticLockException exception) {
            throw versionConflict();
        }
    }

    private UserItemDetailResponse toDetailResponse(
            Long userId,
            UserItem item
    ) {
        List<UserItemImageResponse> images =
                userItemImageRepository
                        .findActiveImages(
                                userId,
                                item.getId()
                        )
                        .stream()
                        .map(this::toImageResponse)
                        .toList();

        Product product = item.getProduct();

        return new UserItemDetailResponse(
                String.valueOf(item.getId()),
                product == null
                        ? null
                        : String.valueOf(product.getId()),
                item.getBrandName(),
                item.getName(),
                item.getCategory(),
                item.getPrimaryColor(),
                item.getMaterial(),
                item.getMaterialSource(),
                item.getPurchaseDate(),
                item.getPurchasePrice(),
                item.getMemo(),
                item.getNextCareDate(),
                item.getAiJobId() == null
                        ? null
                        : String.valueOf(item.getAiJobId()),
                images,
                item.getVersion(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private UserItemImageResponse toImageResponse(
            UserItemImageData image
    ) {
        return new UserItemImageResponse(
                String.valueOf(image.imageId()),
                image.url(),
                image.sortOrder()
        );
    }

    private BusinessException myItemNotFound() {
        return new BusinessException(
                ErrorCode.MY_ITEM_NOT_FOUND
        );
    }

    private BusinessException versionConflict() {
        return new BusinessException(
                ErrorCode.RESOURCE_VERSION_CONFLICT
        );
    }
}
