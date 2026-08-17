package org.likelionhsu.hackathon.styleplan.service;

import java.util.HashSet;
import java.util.Set;

import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.product.entity.ProductStatus;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanCreateRequest;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanCreateResponse;
import org.likelionhsu.hackathon.styleplan.repository.StylePlanPersistenceRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StylePlanService {

    private final StylePlanPersistenceRepository
            persistenceRepository;
    private final AiJobJdbcRepository aiJobRepository;
    private final UserItemRepository userItemRepository;
    private final ProductRepository productRepository;
    private final StylePlanPreviewSourceValidator
            previewSourceValidator;

    public StylePlanService(
            StylePlanPersistenceRepository
                    persistenceRepository,
            AiJobJdbcRepository aiJobRepository,
            UserItemRepository userItemRepository,
            ProductRepository productRepository,
            StylePlanPreviewSourceValidator
                    previewSourceValidator
    ) {
        this.persistenceRepository =
                persistenceRepository;
        this.aiJobRepository = aiJobRepository;
        this.userItemRepository = userItemRepository;
        this.productRepository = productRepository;
        this.previewSourceValidator =
                previewSourceValidator;
    }

    @Transactional
    public StylePlanCreateResponse create(
            Long userId,
            StylePlanCreateRequest request
    ) {
        validateDuplicates(request);

        StylePlanGenerationType generationType =
                determineGenerationType(
                        userId,
                        request
                );

        validateOwnedItems(
                userId,
                request
        );
        validateProducts(request);

        try {
            long stylePlanId =
                    persistenceRepository.insertPlan(
                            userId,
                            request.title(),
                            request.occasion(),
                            request.plannedAt(),
                            request.weatherCondition(),
                            request.description(),
                            generationType,
                            request.status(),
                            request.aiJobId()
                    );

            for (var item : request.ownedItems()) {
                persistenceRepository.insertItem(
                        stylePlanId,
                        item.myItemId(),
                        item.role(),
                        item.sortOrder()
                );
            }

            for (var product :
                    request.recommendedProducts()) {
                persistenceRepository.insertProduct(
                        stylePlanId,
                        product.productId(),
                        product.rank(),
                        product.reason()
                );
            }

            return StylePlanCreateResponse.from(
                    stylePlanId
            );
        } catch (DataIntegrityViolationException exception) {
            if (request.aiJobId() != null) {
                throw new RequestValidationException(
                        "aiJobId",
                        "이미 저장에 사용된 AI Job입니다."
                );
            }

            throw exception;
        }
    }

    private StylePlanGenerationType
            determineGenerationType(
            Long userId,
            StylePlanCreateRequest request
    ) {
        if (request.aiJobId() == null) {
            return StylePlanGenerationType.MANUAL;
        }

        if (persistenceRepository.existsByAiJobId(
                request.aiJobId()
        )) {
            throw new RequestValidationException(
                    "aiJobId",
                    "이미 저장에 사용된 AI Job입니다."
            );
        }

        AiJobData job = aiJobRepository
                .findOwned(
                        userId,
                        request.aiJobId()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.AI_JOB_NOT_FOUND
                        )
                );

        return previewSourceValidator.validate(
                job,
                request
        );
    }

    private void validateOwnedItems(
            Long userId,
            StylePlanCreateRequest request
    ) {
        for (var item : request.ownedItems()) {
            userItemRepository
                    .findByIdAndUser_IdAndDeletedAtIsNull(
                            item.myItemId(),
                            userId
                    )
                    .orElseThrow(() ->
                            new BusinessException(
                                    ErrorCode.MY_ITEM_NOT_FOUND
                            )
                    );
        }
    }

    private void validateProducts(
            StylePlanCreateRequest request
    ) {
        for (var productRequest :
                request.recommendedProducts()) {
            Product product = productRepository
                    .findByIdAndStatus(
                            productRequest.productId(),
                            ProductStatus.ACTIVE
                    )
                    .orElseThrow(() ->
                            new BusinessException(
                                    ErrorCode.PRODUCT_NOT_FOUND
                            )
                    );

            if (product.getBrand()
                    != ProductBrand.MCM) {
                throw new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND
                );
            }
        }
    }

    private void validateDuplicates(
            StylePlanCreateRequest request
    ) {
        Set<Long> itemIds = new HashSet<>();
        Set<Integer> sortOrders = new HashSet<>();

        for (var item : request.ownedItems()) {
            if (!itemIds.add(item.myItemId())) {
                throw duplicate(
                        "ownedItems",
                        "같은 보유 아이템을 중복으로 선택할 수 없습니다."
                );
            }

            if (!sortOrders.add(
                    item.sortOrder()
            )) {
                throw duplicate(
                        "ownedItems",
                        "보유 아이템 sortOrder는 중복될 수 없습니다."
                );
            }
        }

        Set<Long> productIds = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();

        for (var product :
                request.recommendedProducts()) {
            if (!productIds.add(
                    product.productId()
            )) {
                throw duplicate(
                        "recommendedProducts",
                        "같은 추천 상품을 중복으로 선택할 수 없습니다."
                );
            }

            if (!ranks.add(product.rank())) {
                throw duplicate(
                        "recommendedProducts",
                        "추천 상품 rank는 중복될 수 없습니다."
                );
            }
        }
    }

    private RequestValidationException duplicate(
            String field,
            String reason
    ) {
        return new RequestValidationException(
                field,
                reason
        );
    }
}
