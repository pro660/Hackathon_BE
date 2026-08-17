package org.likelionhsu.hackathon.aijob.service;

import java.util.Objects;

import tools.jackson.databind.ObjectMapper;

import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;
import org.likelionhsu.hackathon.aijob.dto.response.AiJobCreateResponse;
import org.likelionhsu.hackathon.aijob.dto.response.AiJobResponse;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.itemanalysis.service.ItemAnalysisAiJobDispatcher;
import org.likelionhsu.hackathon.purchaseutility.service.PurchaseUtilityAiJobDispatcher;
import org.likelionhsu.hackathon.styleplan.service.StylePlanAiJobDispatcher;
import org.likelionhsu.hackathon.styleplan.service.StylePlanJobRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AiJobService {

    private static final String PURCHASE_UTILITY_PROMPT_VERSION =
            "purchase-utility-summary-v1";
    private static final String ITEM_ANALYSIS_PROMPT_VERSION =
            "item-analysis-v1";
    private static final String STYLE_PLAN_PROMPT_VERSION =
            "style-plan-v1";

    private final AiJobJdbcRepository aiJobRepository;
    private final AiJobRequestHasher requestHasher;
    private final ObjectMapper objectMapper;
    private final ItemAnalysisAiJobCreationService
            itemAnalysisAiJobCreationService;
    private final ItemAnalysisAiJobDispatcher
            itemAnalysisAiJobDispatcher;
    private final PurchaseUtilityAiJobDispatcher
            purchaseUtilityAiJobDispatcher;
    private final StylePlanAiJobDispatcher
            stylePlanAiJobDispatcher;
    private final AiJobCreationPolicyService
            creationPolicyService;
    private final String openAiModel;

    @Autowired
    public AiJobService(
            AiJobJdbcRepository aiJobRepository,
            AiJobRequestHasher requestHasher,
            ObjectMapper objectMapper,
            ItemAnalysisAiJobCreationService
                    itemAnalysisAiJobCreationService,
            ItemAnalysisAiJobDispatcher
                    itemAnalysisAiJobDispatcher,
            PurchaseUtilityAiJobDispatcher
                    purchaseUtilityAiJobDispatcher,
            StylePlanAiJobDispatcher
                    stylePlanAiJobDispatcher,
            AiJobCreationPolicyService
                    creationPolicyService,
            @Value("${OPENAI_MODEL:}")
            String openAiModel
    ) {
        this.aiJobRepository = aiJobRepository;
        this.requestHasher = requestHasher;
        this.objectMapper = objectMapper;
        this.itemAnalysisAiJobCreationService =
                itemAnalysisAiJobCreationService;
        this.itemAnalysisAiJobDispatcher =
                itemAnalysisAiJobDispatcher;
        this.purchaseUtilityAiJobDispatcher =
                purchaseUtilityAiJobDispatcher;
        this.stylePlanAiJobDispatcher =
                stylePlanAiJobDispatcher;
        this.creationPolicyService =
                creationPolicyService;
        this.openAiModel = openAiModel;
    }

    /*
     * Existing unit-test compatibility constructors.
     * Production uses the @Autowired constructor above.
     */
    AiJobService(
            AiJobJdbcRepository aiJobRepository,
            AiJobRequestHasher requestHasher,
            ObjectMapper objectMapper,
            ItemAnalysisAiJobCreationService
                    itemAnalysisAiJobCreationService,
            ItemAnalysisAiJobDispatcher
                    itemAnalysisAiJobDispatcher,
            PurchaseUtilityAiJobDispatcher
                    purchaseUtilityAiJobDispatcher,
            StylePlanAiJobDispatcher
                    stylePlanAiJobDispatcher,
            String openAiModel
    ) {
        this(
                aiJobRepository,
                requestHasher,
                objectMapper,
                itemAnalysisAiJobCreationService,
                itemAnalysisAiJobDispatcher,
                purchaseUtilityAiJobDispatcher,
                stylePlanAiJobDispatcher,
                null,
                openAiModel
        );
    }

    AiJobService(
            AiJobJdbcRepository aiJobRepository,
            AiJobRequestHasher requestHasher,
            ObjectMapper objectMapper,
            ItemAnalysisAiJobCreationService
                    itemAnalysisAiJobCreationService,
            ItemAnalysisAiJobDispatcher
                    itemAnalysisAiJobDispatcher,
            PurchaseUtilityAiJobDispatcher
                    purchaseUtilityAiJobDispatcher,
            String openAiModel
    ) {
        this(
                aiJobRepository,
                requestHasher,
                objectMapper,
                itemAnalysisAiJobCreationService,
                itemAnalysisAiJobDispatcher,
                purchaseUtilityAiJobDispatcher,
                null,
                null,
                openAiModel
        );
    }

    public CreationResult create(
            Long userId,
            String idempotencyKey,
            AiJobCreateRequest request
    ) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );

        if (request != null
                && request.type() == AiJobType.ITEM_ANALYSIS) {
            return createItemAnalysis(
                    userId,
                    idempotencyKey,
                    request
            );
        }

        if (request != null
                && request.type() == AiJobType.STYLE_PLAN) {
            return createStylePlan(
                    userId,
                    idempotencyKey,
                    request
            );
        }

        return createPurchaseUtility(
                userId,
                idempotencyKey,
                request
        );
    }

    private CreationResult createPurchaseUtility(
            Long userId,
            String idempotencyKey,
            AiJobCreateRequest request
    ) {
        String normalizedProductId =
                normalizePurchaseUtilityProductId(request);

        String requestHash =
                requestHasher.hashPurchaseUtility(
                        normalizedProductId
                );

        var existing =
                aiJobRepository.findByUserAndIdempotencyKey(
                        userId,
                        idempotencyKey
                );

        if (existing.isPresent()) {
            return resolveExisting(
                    existing.get(),
                    requestHash
            );
        }

        String model = requireConfiguredModel();

        try {
            long jobId = createWithinPolicy(
                    userId,
                    idempotencyKey,
                    () -> aiJobRepository.createPending(
                            userId,
                            AiJobType.PURCHASE_UTILITY,
                            idempotencyKey,
                            requestHash,
                            model,
                            PURCHASE_UTILITY_PROMPT_VERSION
                    )
            );

            AiJobData created = aiJobRepository
                    .findOwned(userId, jobId)
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "생성한 AI Job을 조회할 수 없습니다."
                            )
                    );

            purchaseUtilityAiJobDispatcher.dispatch(
                    userId,
                    jobId,
                    Long.valueOf(normalizedProductId)
            );

            return CreationResult.from(created);
        } catch (DataIntegrityViolationException exception) {
            return resolveWinnerAfterInsertRace(
                    userId,
                    idempotencyKey,
                    requestHash,
                    exception
            );
        }
    }

    private CreationResult createItemAnalysis(
            Long userId,
            String idempotencyKey,
            AiJobCreateRequest request
    ) {
        String normalizedImageAssetId =
                normalizeItemAnalysisImageAssetId(request);

        String requestHash =
                requestHasher.hashItemAnalysis(
                        normalizedImageAssetId
                );

        var existing =
                aiJobRepository.findByUserAndIdempotencyKey(
                        userId,
                        idempotencyKey
                );

        if (existing.isPresent()) {
            return resolveExisting(
                    existing.get(),
                    requestHash
            );
        }

        String model = requireConfiguredModel();

        try {
            Long imageAssetId =
                    Long.valueOf(normalizedImageAssetId);

            AiJobData created =
                    createWithinPolicy(
                            userId,
                            idempotencyKey,
                            () -> itemAnalysisAiJobCreationService
                                    .createPendingAndBind(
                                            userId,
                                            imageAssetId,
                                            idempotencyKey,
                                            requestHash,
                                            model,
                                            ITEM_ANALYSIS_PROMPT_VERSION
                                    )
                    );

            itemAnalysisAiJobDispatcher.dispatch(
                    userId,
                    created.id(),
                    imageAssetId
            );

            return CreationResult.from(created);
        } catch (DataIntegrityViolationException exception) {
            return resolveWinnerAfterInsertRace(
                    userId,
                    idempotencyKey,
                    requestHash,
                    exception
            );
        }
    }

    private CreationResult createStylePlan(
            Long userId,
            String idempotencyKey,
            AiJobCreateRequest request
    ) {
        StylePlanJobRequest stylePlanRequest =
                StylePlanJobRequest.from(request);

        String requestHash = requestHasher.hashStylePlan(
                stylePlanRequest.occasion(),
                stylePlanRequest.styleTags(),
                stylePlanRequest.weatherCondition(),
                stylePlanRequest.prioritizeOwnedItems(),
                stylePlanRequest.language()
        );

        var existing =
                aiJobRepository.findByUserAndIdempotencyKey(
                        userId,
                        idempotencyKey
                );

        if (existing.isPresent()) {
            return resolveExisting(
                    existing.get(),
                    requestHash
            );
        }

        String model = requireConfiguredModel();

        try {
            long jobId = createWithinPolicy(
                    userId,
                    idempotencyKey,
                    () -> aiJobRepository.createPending(
                            userId,
                            AiJobType.STYLE_PLAN,
                            idempotencyKey,
                            requestHash,
                            model,
                            STYLE_PLAN_PROMPT_VERSION
                    )
            );

            AiJobData created = aiJobRepository
                    .findOwned(userId, jobId)
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "생성한 STYLE_PLAN AI Job을 조회할 수 없습니다."
                            )
                    );

            stylePlanAiJobDispatcher.dispatch(
                    userId,
                    jobId,
                    stylePlanRequest
            );

            return CreationResult.from(created);
        } catch (DataIntegrityViolationException exception) {
            return resolveWinnerAfterInsertRace(
                    userId,
                    idempotencyKey,
                    requestHash,
                    exception
            );
        }
    }

    public AiJobResponse get(
            Long userId,
            Long jobId
    ) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                jobId,
                "jobId는 null일 수 없습니다."
        );

        AiJobData job = aiJobRepository
                .findOwned(userId, jobId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.AI_JOB_NOT_FOUND
                        )
                );

        return AiJobResponse.from(
                refreshStaleIfNeeded(job),
                objectMapper
        );
    }

    private CreationResult resolveWinnerAfterInsertRace(
            Long userId,
            String idempotencyKey,
            String requestHash,
            DataIntegrityViolationException original
    ) {
        return aiJobRepository
                .findByUserAndIdempotencyKey(
                        userId,
                        idempotencyKey
                )
                .map(job -> resolveExisting(
                        job,
                        requestHash
                ))
                .orElseThrow(() -> original);
    }

    private CreationResult resolveExisting(
            AiJobData existing,
            String requestHash
    ) {
        if (!requestHash.equals(existing.requestHash())) {
            throw new BusinessException(
                    ErrorCode.IDEMPOTENCY_KEY_CONFLICT
            );
        }

        return CreationResult.from(
                refreshStaleIfNeeded(existing)
        );
    }

    private AiJobData refreshStaleIfNeeded(
            AiJobData job
    ) {
        if (job.status() != AiJobStatus.PENDING
                && job.status() != AiJobStatus.PROCESSING) {
            return job;
        }

        boolean timedOut =
                aiJobRepository.markTimedOutIfStale(
                        job.userId(),
                        job.id()
                );

        if (!timedOut) {
            return job;
        }

        return aiJobRepository
                .findOwned(job.userId(), job.id())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.AI_JOB_NOT_FOUND
                        )
                );
    }

    private String normalizePurchaseUtilityProductId(
            AiJobCreateRequest request
    ) {
        if (request == null
                || request.type() != AiJobType.PURCHASE_UTILITY
                || request.context() == null) {
            throw new BusinessException(
                    ErrorCode.REQUEST_BODY_INVALID
            );
        }

        return normalizePositiveId(
                request.context().productId(),
                "context.productId"
        );
    }

    private String normalizeItemAnalysisImageAssetId(
            AiJobCreateRequest request
    ) {
        if (request == null
                || request.type() != AiJobType.ITEM_ANALYSIS
                || request.context() == null) {
            throw new BusinessException(
                    ErrorCode.REQUEST_BODY_INVALID
            );
        }

        return normalizePositiveId(
                request.context().imageAssetId(),
                "context.imageAssetId"
        );
    }

    private String normalizePositiveId(
            String rawValue,
            String field
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new RequestValidationException(
                    field,
                    "필수 입력값입니다."
            );
        }

        try {
            long value = Long.parseLong(rawValue.trim());

            if (value <= 0L) {
                throw new NumberFormatException(
                        "id must be positive"
                );
            }

            return String.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new RequestValidationException(
                    field,
                    "1 이상의 정수로 입력해 주세요."
            );
        }
    }

    private <T> T createWithinPolicy(
            Long userId,
            String idempotencyKey,
            java.util.function.Supplier<T> creation
    ) {
        if (creationPolicyService == null) {
            return creation.get();
        }

        return creationPolicyService.execute(
                userId,
                idempotencyKey,
                creation
        );
    }

    private String requireConfiguredModel() {
        if (openAiModel == null
                || openAiModel.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_MODEL 환경변수가 설정되지 않았습니다."
            );
        }

        return openAiModel.trim();
    }

    public record CreationResult(
            AiJobCreateResponse response,
            boolean accepted
    ) {

        public static CreationResult from(
                AiJobData job
        ) {
            boolean accepted =
                    job.status() == AiJobStatus.PENDING
                            || job.status()
                            == AiJobStatus.PROCESSING;

            return new CreationResult(
                    AiJobCreateResponse.from(job),
                    accepted
            );
        }
    }
}
