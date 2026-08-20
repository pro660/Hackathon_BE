package org.likelionhsu.hackathon.styleplan.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;

public record StylePlanJobRequest(
        String occasion,
        int casualFormalLevel,
        int neatGlamorousLevel,
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

    public StylePlanJobRequest(
            String occasion,
            List<String> styleTags,
            String weatherCondition,
            boolean prioritizeOwnedItems,
            String language
    ) {
        this(
                occasion,
                deriveAxisLevel(styleTags, "CASUAL", "FORMAL"),
                deriveAxisLevel(styleTags, "NEAT", "GLAMOROUS"),
                styleTags,
                weatherCondition,
                prioritizeOwnedItems,
                language
        );
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
                || context.prioritizeOwnedItems() == null
                || context.language() == null) {
            throw new BusinessException(
                    ErrorCode.REQUEST_BODY_INVALID
            );
        }

        List<String> normalizedStyleTags =
                context.styleTags() == null
                        ? List.of()
                        : context.styleTags()
                                .stream()
                                .map(String::trim)
                                .sorted()
                                .toList();

        int casualFormalLevel =
                context.casualFormalLevel() == null
                        ? deriveAxisLevel(
                                normalizedStyleTags,
                                "CASUAL",
                                "FORMAL"
                        )
                        : context.casualFormalLevel();

        int neatGlamorousLevel =
                context.neatGlamorousLevel() == null
                        ? deriveAxisLevel(
                                normalizedStyleTags,
                                "NEAT",
                                "GLAMOROUS"
                        )
                        : context.neatGlamorousLevel();

        if (normalizedStyleTags.isEmpty()) {
            normalizedStyleTags = deriveStyleTags(
                    casualFormalLevel,
                    neatGlamorousLevel
            );
        }

        String normalizedWeather =
                context.weatherCondition() == null
                        ? null
                        : context.weatherCondition().trim();

        return new StylePlanJobRequest(
                context.occasion().trim(),
                casualFormalLevel,
                neatGlamorousLevel,
                normalizedStyleTags,
                normalizedWeather,
                context.prioritizeOwnedItems(),
                context.language().trim()
        );
    }

    private static int deriveAxisLevel(
            List<String> styleTags,
            String lowTag,
            String highTag
    ) {
        if (styleTags == null || styleTags.isEmpty()) {
            return 5;
        }

        boolean low = styleTags.stream()
                .map(String::trim)
                .anyMatch(lowTag::equals);
        boolean high = styleTags.stream()
                .map(String::trim)
                .anyMatch(highTag::equals);

        if (low == high) {
            return 5;
        }

        return high ? 8 : 3;
    }

    private static List<String> deriveStyleTags(
            int casualFormalLevel,
            int neatGlamorousLevel
    ) {
        List<String> result = new ArrayList<>();

        if (casualFormalLevel <= 4) {
            result.add("CASUAL");
        } else if (casualFormalLevel >= 7) {
            result.add("FORMAL");
        }

        if (neatGlamorousLevel <= 4) {
            result.add("NEAT");
        } else if (neatGlamorousLevel >= 7) {
            result.add("GLAMOROUS");
        }

        return List.copyOf(result);
    }
}
