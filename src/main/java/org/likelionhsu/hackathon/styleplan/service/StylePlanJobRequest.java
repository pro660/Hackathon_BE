package org.likelionhsu.hackathon.styleplan.service;

import java.util.List;
import java.util.Objects;

import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;

public record StylePlanJobRequest(
        String occasion,
        List<String> styleTags,
        String weatherCondition,
        boolean prioritizeOwnedItems,
        String language
) {

    public StylePlanJobRequest {
        Objects.requireNonNull(occasion, "occasion");
        Objects.requireNonNull(styleTags, "styleTags");
        Objects.requireNonNull(language, "language");
        styleTags = List.copyOf(styleTags);
    }

    public static StylePlanJobRequest from(
            AiJobCreateRequest request
    ) {
        if (request == null
                || request.type() != AiJobType.STYLE_PLAN
                || request.context() == null) {
            throw new BusinessException(
                    ErrorCode.REQUEST_BODY_INVALID
            );
        }

        AiJobCreateRequest.Context context =
                request.context();

        if (context.occasion() == null
                || context.styleTags() == null
                || context.prioritizeOwnedItems() == null
                || context.language() == null) {
            throw new BusinessException(
                    ErrorCode.REQUEST_BODY_INVALID
            );
        }

        List<String> normalizedStyleTags =
                context.styleTags()
                        .stream()
                        .map(String::trim)
                        .sorted()
                        .toList();

        String normalizedWeather =
                context.weatherCondition() == null
                        ? null
                        : context.weatherCondition().trim();

        return new StylePlanJobRequest(
                context.occasion().trim(),
                normalizedStyleTags,
                normalizedWeather,
                context.prioritizeOwnedItems(),
                context.language().trim()
        );
    }
}
