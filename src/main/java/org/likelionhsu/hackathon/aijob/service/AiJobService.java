package org.likelionhsu.hackathon.aijob.service;

import java.util.Objects;

import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;
import org.likelionhsu.hackathon.aijob.dto.response.AiJobCreateResponse;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AiJobService {

    private static final String PURCHASE_UTILITY_PROMPT_VERSION =
            "purchase-utility-summary-v1";

    private final AiJobJdbcRepository aiJobRepository;
    private final AiJobRequestHasher requestHasher;
    private final String openAiModel;

    public AiJobService(
            AiJobJdbcRepository aiJobRepository,
            AiJobRequestHasher requestHasher,
            @Value("${OPENAI_MODEL:}")
            String openAiModel
    ) {
        this.aiJobRepository = aiJobRepository;
        this.requestHasher = requestHasher;
        this.openAiModel = openAiModel;
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

        String normalizedProductId =
                normalizePurchaseUtilityProductId(request);

        String requestHash =
                requestHasher.hashPurchaseUtility(
                        normalizedProductId
                );

        var existing =
                aiJobRepository
                        .findByUserAndIdempotencyKey(
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
            long jobId =
                    aiJobRepository.createPending(
                            userId,
                            AiJobType.PURCHASE_UTILITY,
                            idempotencyKey,
                            requestHash,
                            model,
                            PURCHASE_UTILITY_PROMPT_VERSION
                    );

            AiJobData created =
                    aiJobRepository
                            .findOwned(
                                    userId,
                                    jobId
                            )
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "생성한 AI Job을 조회할 수 없습니다."
                                    )
                            );

            return CreationResult.from(created);
        } catch (DataIntegrityViolationException exception) {
            return aiJobRepository
                    .findByUserAndIdempotencyKey(
                            userId,
                            idempotencyKey
                    )
                    .map(job ->
                            resolveExisting(
                                    job,
                                    requestHash
                            )
                    )
                    .orElseThrow(() -> exception);
        }
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

        return CreationResult.from(existing);
    }

    private String normalizePurchaseUtilityProductId(
            AiJobCreateRequest request
    ) {
        if (request == null
                || request.type()
                != AiJobType.PURCHASE_UTILITY
                || request.context() == null) {
            throw new BusinessException(
                    ErrorCode.REQUEST_BODY_INVALID
            );
        }

        String rawProductId =
                request.context().productId();

        if (rawProductId == null
                || rawProductId.isBlank()) {
            throw new RequestValidationException(
                    "context.productId",
                    "필수 입력값입니다."
            );
        }

        try {
            long productId =
                    Long.parseLong(rawProductId.trim());

            if (productId <= 0L) {
                throw new NumberFormatException(
                        "productId must be positive"
                );
            }

            return String.valueOf(productId);
        } catch (NumberFormatException exception) {
            throw new RequestValidationException(
                    "context.productId",
                    "1 이상의 정수로 입력해 주세요."
            );
        }
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
