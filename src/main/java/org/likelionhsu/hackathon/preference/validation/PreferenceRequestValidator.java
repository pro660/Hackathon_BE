package org.likelionhsu.hackathon.preference.validation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.preference.dto.request.PreferenceRequest;
import org.likelionhsu.hackathon.preference.entity.PreferenceStyleTag;
import org.springframework.stereotype.Component;

@Component
public class PreferenceRequestValidator {

    private static final String EMPTY_VALUE_REASON =
            "빈 값은 사용할 수 없습니다.";

    private static final String DUPLICATE_VALUE_REASON =
            "중복된 값은 선택할 수 없습니다.";

    private static final String INVALID_VALUE_REASON =
            "허용되지 않은 값이 포함되어 있습니다.";

    private static final Set<String> ALLOWED_COLORS =
            enumNames(ColorGroup.values());

    private static final Set<String> ALLOWED_CATEGORIES =
            enumNames(ItemCategory.values());

    private static final Set<String> ALLOWED_STYLE_TAGS =
            enumNames(PreferenceStyleTag.values());

    public ValidatedPreferenceRequest validate(
            PreferenceRequest request
    ) {
        validateValues(
                "preferredColors",
                request.preferredColors(),
                ALLOWED_COLORS
        );

        validateValues(
                "preferredCategories",
                request.preferredCategories(),
                ALLOWED_CATEGORIES
        );

        validateValues(
                "preferredStyleTags",
                request.preferredStyleTags(),
                ALLOWED_STYLE_TAGS
        );

        List<ColorGroup> preferredColors =
                request.preferredColors()
                        .stream()
                        .map(ColorGroup::valueOf)
                        .sorted(
                                Comparator.comparing(
                                        Enum::name
                                )
                        )
                        .toList();

        List<ItemCategory> preferredCategories =
                request.preferredCategories()
                        .stream()
                        .map(ItemCategory::valueOf)
                        .sorted(
                                Comparator.comparing(
                                        Enum::name
                                )
                        )
                        .toList();

        List<PreferenceStyleTag> preferredStyleTags =
                request.preferredStyleTags()
                        .stream()
                        .map(PreferenceStyleTag::valueOf)
                        .sorted(
                                Comparator.comparing(
                                        Enum::name
                                )
                        )
                        .toList();

        return new ValidatedPreferenceRequest(
                preferredColors,
                preferredCategories,
                preferredStyleTags
        );
    }

    private void validateValues(
            String field,
            List<String> values,
            Set<String> allowedValues
    ) {
        Set<String> seenValues =
                new HashSet<>();

        for (String value : values) {
            if (value == null
                    || value.isBlank()) {

                throw new RequestValidationException(
                        field,
                        EMPTY_VALUE_REASON
                );
            }

            if (!seenValues.add(value)) {
                throw new RequestValidationException(
                        field,
                        DUPLICATE_VALUE_REASON
                );
            }

            if (!allowedValues.contains(value)) {
                throw new RequestValidationException(
                        field,
                        INVALID_VALUE_REASON
                );
            }
        }
    }

    private static Set<String> enumNames(
            Enum<?>[] values
    ) {
        return java.util.Arrays
                .stream(values)
                .map(Enum::name)
                .collect(
                        java.util.stream.Collectors.toUnmodifiableSet()
                );
    }

    public record ValidatedPreferenceRequest(
            List<ColorGroup> preferredColors,
            List<ItemCategory> preferredCategories,
            List<PreferenceStyleTag> preferredStyleTags
    ) {

        public ValidatedPreferenceRequest {
            preferredColors =
                    List.copyOf(preferredColors);

            preferredCategories =
                    List.copyOf(preferredCategories);

            preferredStyleTags =
                    List.copyOf(preferredStyleTags);
        }
    }
}