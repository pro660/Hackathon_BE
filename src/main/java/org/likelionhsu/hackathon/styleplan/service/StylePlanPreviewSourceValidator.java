package org.likelionhsu.hackathon.styleplan.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanCreateRequest;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class StylePlanPreviewSourceValidator {

    private final ObjectMapper objectMapper;

    public StylePlanPreviewSourceValidator(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    public StylePlanGenerationType validate(
            AiJobData job,
            StylePlanCreateRequest request
    ) {
        if (job.type() != AiJobType.STYLE_PLAN) {
            throw invalidAiJob();
        }

        String previewJson;
        StylePlanGenerationType generationType;

        if (job.status() == AiJobStatus.SUCCEEDED
                && hasText(job.resultJson())) {
            previewJson = job.resultJson();
            generationType = StylePlanGenerationType.AI;
        } else if (job.status() == AiJobStatus.FAILED
                && hasText(job.fallbackJson())) {
            previewJson = job.fallbackJson();
            generationType =
                    StylePlanGenerationType.RULE_BASED;
        } else {
            throw invalidAiJob();
        }

        JsonNode preview = readPreview(previewJson);

        String declaredGenerationType =
                preview.path("generationType")
                        .asString();

        if (!generationType.name().equals(
                declaredGenerationType
        )) {
            throw invalidAiJob();
        }

        validateOwnedItems(
                preview.path("ownedItems"),
                request
        );
        validateProducts(
                preview.path("recommendedProducts"),
                request
        );

        return generationType;
    }

    private void validateOwnedItems(
            JsonNode previewItems,
            StylePlanCreateRequest request
    ) {
        if (!previewItems.isArray()) {
            throw invalidAiJob();
        }

        Map<Long, PreviewOwnedItem> expected =
                new HashMap<>();

        for (JsonNode item : previewItems) {
            long id = parsePositiveId(
                    item.path("myItemId").asString()
            );

            PreviewOwnedItem previous =
                    expected.put(
                            id,
                            new PreviewOwnedItem(
                                    item.path("role")
                                            .asString(),
                                    item.path("sortOrder")
                                            .asInt(-1)
                            )
                    );

            if (previous != null) {
                throw invalidAiJob();
            }
        }

        if (expected.size()
                != request.ownedItems().size()) {
            throw compositionMismatch();
        }

        for (var item : request.ownedItems()) {
            PreviewOwnedItem source =
                    expected.get(item.myItemId());

            if (source == null
                    || !source.role()
                            .equals(item.role().name())
                    || source.sortOrder()
                            != item.sortOrder()) {
                throw compositionMismatch();
            }
        }
    }

    private void validateProducts(
            JsonNode previewProducts,
            StylePlanCreateRequest request
    ) {
        if (!previewProducts.isArray()) {
            throw invalidAiJob();
        }

        Map<Long, Integer> expected =
                new HashMap<>();

        for (JsonNode product : previewProducts) {
            long id = parsePositiveId(
                    product.path("productId").asString()
            );

            Integer previous = expected.put(
                    id,
                    product.path("rank").asInt(-1)
            );

            if (previous != null) {
                throw invalidAiJob();
            }
        }

        if (expected.size()
                != request.recommendedProducts().size()) {
            throw compositionMismatch();
        }

        for (var product :
                request.recommendedProducts()) {
            Integer rank =
                    expected.get(product.productId());

            if (rank == null
                    || rank != product.rank()) {
                throw compositionMismatch();
            }
        }
    }

    private JsonNode readPreview(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw invalidAiJob();
        }
    }

    private long parsePositiveId(String value) {
        try {
            long parsed = Long.parseLong(value);

            if (parsed <= 0L) {
                throw invalidAiJob();
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw invalidAiJob();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private RequestValidationException invalidAiJob() {
        return new RequestValidationException(
                "aiJobId",
                "저장 가능한 STYLE_PLAN AI Job이 아닙니다."
        );
    }

    private RequestValidationException compositionMismatch() {
        return new RequestValidationException(
                "aiJobId",
                "AI Job 미리보기와 저장하려는 아이템/상품 조합이 일치하지 않습니다."
        );
    }

    private record PreviewOwnedItem(
            String role,
            int sortOrder
    ) {
    }
}
