package org.likelionhsu.hackathon.recommendation.validation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.recommendation.dto.request.RecommendationRequest;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationFeature;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationOccasion;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationSeason;
import org.springframework.stereotype.Component;

@Component
public class RecommendationRequestValidator {

    private static final String REQUIRED_REASON =
            "필수 입력값입니다.";
    private static final String EMPTY_VALUE_REASON =
            "빈 값은 사용할 수 없습니다.";
    private static final String DUPLICATE_VALUE_REASON =
            "중복된 값은 선택할 수 없습니다.";
    private static final String INVALID_VALUE_REASON =
            "허용되지 않은 값이 포함되어 있습니다.";
    private static final String FEATURE_SIZE_REASON =
            "1개 이상 3개 이하로 선택해 주세요.";
    private static final String LIMIT_RANGE_REASON =
            "1 이상 3 이하로 입력해 주세요.";

    private static final Set<String> ALLOWED_OCCASIONS =
            enumNames(RecommendationOccasion.values());
    private static final Set<String> ALLOWED_SEASONS =
            enumNames(RecommendationSeason.values());
    private static final Set<String> ALLOWED_FEATURES =
            enumNames(RecommendationFeature.values());
    private static final Set<String> ALLOWED_CATEGORIES =
            enumNames(ItemCategory.values());

    public ValidatedRecommendationRequest validate(
            RecommendationRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "request는 null일 수 없습니다."
            );
        }

        validateRequiredScalar(
                "occasion",
                request.occasion(),
                ALLOWED_OCCASIONS
        );
        validateRequiredScalar(
                "season",
                request.season(),
                ALLOWED_SEASONS
        );
        validateFeatures(request.preferredFeatures());
        validateOptionalPresence(
                "category",
                request.categoryProvided(),
                request.category()
        );
        validateOptionalPresence(
                "limit",
                request.limitProvided(),
                request.limit()
        );
        validateOptionalCategory(request.category());
        validateLimit(request.limit());

        List<RecommendationFeature> preferredFeatures =
                request.preferredFeatures()
                        .stream()
                        .map(RecommendationFeature::valueOf)
                        .sorted(Comparator.comparing(Enum::name))
                        .toList();

        return new ValidatedRecommendationRequest(
                RecommendationOccasion.valueOf(
                        request.occasion()
                ),
                RecommendationSeason.valueOf(
                        request.season()
                ),
                preferredFeatures,
                request.category() == null
                        ? null
                        : ItemCategory.valueOf(
                                request.category()
                        ),
                request.limit() == null
                        ? 3
                        : request.limit()
        );
    }

    private void validateOptionalPresence(
            String field,
            boolean provided,
            Object value
    ) {
        if (provided && value == null) {
            throw new RequestValidationException(
                    field,
                    "null은 사용할 수 없습니다."
            );
        }
    }

    private void validateRequiredScalar(
            String field,
            String value,
            Set<String> allowedValues
    ) {
        if (value == null) {
            throw new RequestValidationException(
                    field,
                    REQUIRED_REASON
            );
        }

        if (value.isBlank()) {
            throw new RequestValidationException(
                    field,
                    EMPTY_VALUE_REASON
            );
        }

        if (!allowedValues.contains(value)) {
            throw new RequestValidationException(
                    field,
                    INVALID_VALUE_REASON
            );
        }
    }

    private void validateFeatures(List<String> values) {
        if (values == null) {
            throw new RequestValidationException(
                    "preferredFeatures",
                    REQUIRED_REASON
            );
        }

        if (values.size() < 1 || values.size() > 3) {
            throw new RequestValidationException(
                    "preferredFeatures",
                    FEATURE_SIZE_REASON
            );
        }

        Set<String> seenValues = new HashSet<>();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new RequestValidationException(
                        "preferredFeatures",
                        EMPTY_VALUE_REASON
                );
            }

            if (!seenValues.add(value)) {
                throw new RequestValidationException(
                        "preferredFeatures",
                        DUPLICATE_VALUE_REASON
                );
            }

            if (!ALLOWED_FEATURES.contains(value)) {
                throw new RequestValidationException(
                        "preferredFeatures",
                        INVALID_VALUE_REASON
                );
            }
        }
    }

    private void validateOptionalCategory(String category) {
        if (category == null) {
            return;
        }

        if (category.isBlank()) {
            throw new RequestValidationException(
                    "category",
                    EMPTY_VALUE_REASON
            );
        }

        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new RequestValidationException(
                    "category",
                    INVALID_VALUE_REASON
            );
        }
    }

    private void validateLimit(Integer limit) {
        if (limit == null) {
            return;
        }

        if (limit < 1 || limit > 3) {
            throw new RequestValidationException(
                    "limit",
                    LIMIT_RANGE_REASON
            );
        }
    }

    private static Set<String> enumNames(Enum<?>[] values) {
        return Arrays
                .stream(values)
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    public record ValidatedRecommendationRequest(
            RecommendationOccasion occasion,
            RecommendationSeason season,
            List<RecommendationFeature> preferredFeatures,
            ItemCategory category,
            int limit
    ) {
        public ValidatedRecommendationRequest {
            preferredFeatures = List.copyOf(preferredFeatures);
        }
    }
}
