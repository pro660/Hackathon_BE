package org.likelionhsu.hackathon.aijob.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AiJobContextValidator
        implements ConstraintValidator<
        ValidAiJobContext,
        AiJobCreateRequest
        > {

    private static final String REQUIRED_MESSAGE =
            "필수 입력값입니다.";
    private static final String NOT_ALLOWED_MESSAGE =
            "해당 AI 작업 타입에서는 사용할 수 없습니다.";
    private static final String INVALID_VALUE_MESSAGE =
            "허용되지 않는 값입니다.";
    private static final String STYLE_TAG_SIZE_MESSAGE =
            "1개 이상 4개 이하로 선택해 주세요.";
    private static final String DUPLICATE_MESSAGE =
            "중복 값을 사용할 수 없습니다.";

    private static final Set<String> OCCASIONS = Set.of(
            "DAILY",
            "DATE",
            "TRAVEL",
            "GATHERING",
            "CEREMONY",
            "OUTDOOR",
            "OTHER"
    );

    private static final Set<String> STYLE_TAGS = Set.of(
            "CASUAL",
            "FORMAL",
            "NEAT",
            "GLAMOROUS"
    );

    private static final Set<String> WEATHER_CONDITIONS = Set.of(
            "SUNNY",
            "CLOUDY",
            "RAINY",
            "SNOWY",
            "HOT",
            "COLD",
            "WINDY",
            "INDOOR",
            "OTHER"
    );

    @Override
    public boolean isValid(
            AiJobCreateRequest request,
            ConstraintValidatorContext context
    ) {
        if (request == null
                || request.type() == null
                || request.context() == null) {
            return true;
        }

        return switch (request.type()) {
            case PURCHASE_UTILITY ->
                    validatePurchaseUtility(
                            request.context(),
                            context
                    );
            case ITEM_ANALYSIS ->
                    validateItemAnalysis(
                            request.context(),
                            context
                    );
            case STYLE_PLAN ->
                    validateStylePlan(
                            request.context(),
                            context
                    );
        };
    }

    private boolean validatePurchaseUtility(
            AiJobCreateRequest.Context requestContext,
            ConstraintValidatorContext context
    ) {
        boolean valid = true;

        if (!hasText(requestContext.productId())) {
            addViolation(
                    context,
                    "productId",
                    REQUIRED_MESSAGE
            );
            valid = false;
        }

        if (hasText(requestContext.imageAssetId())) {
            addViolation(
                    context,
                    "imageAssetId",
                    NOT_ALLOWED_MESSAGE
            );
            valid = false;
        }

        return validateNoStylePlanFields(
                requestContext,
                context,
                valid
        );
    }

    private boolean validateItemAnalysis(
            AiJobCreateRequest.Context requestContext,
            ConstraintValidatorContext context
    ) {
        boolean valid = true;

        if (!hasText(requestContext.imageAssetId())) {
            addViolation(
                    context,
                    "imageAssetId",
                    REQUIRED_MESSAGE
            );
            valid = false;
        }

        if (hasText(requestContext.productId())) {
            addViolation(
                    context,
                    "productId",
                    NOT_ALLOWED_MESSAGE
            );
            valid = false;
        }

        return validateNoStylePlanFields(
                requestContext,
                context,
                valid
        );
    }

    private boolean validateStylePlan(
            AiJobCreateRequest.Context requestContext,
            ConstraintValidatorContext context
    ) {
        boolean valid = true;

        if (hasText(requestContext.productId())) {
            addViolation(
                    context,
                    "productId",
                    NOT_ALLOWED_MESSAGE
            );
            valid = false;
        }

        if (hasText(requestContext.imageAssetId())) {
            addViolation(
                    context,
                    "imageAssetId",
                    NOT_ALLOWED_MESSAGE
            );
            valid = false;
        }

        if (!hasText(requestContext.occasion())) {
            addViolation(
                    context,
                    "occasion",
                    REQUIRED_MESSAGE
            );
            valid = false;
        } else if (!OCCASIONS.contains(
                requestContext.occasion().trim()
        )) {
            addViolation(
                    context,
                    "occasion",
                    INVALID_VALUE_MESSAGE
            );
            valid = false;
        }

        Integer casualFormalLevel =
                requestContext.casualFormalLevel();
        Integer neatGlamorousLevel =
                requestContext.neatGlamorousLevel();
        boolean hasSliderLevels =
                casualFormalLevel != null
                        || neatGlamorousLevel != null;

        if (hasSliderLevels) {
            if (casualFormalLevel == null) {
                addViolation(
                        context,
                        "casualFormalLevel",
                        REQUIRED_MESSAGE
                );
                valid = false;
            } else if (!isSliderLevel(casualFormalLevel)) {
                addViolation(
                        context,
                        "casualFormalLevel",
                        INVALID_VALUE_MESSAGE
                );
                valid = false;
            }

            if (neatGlamorousLevel == null) {
                addViolation(
                        context,
                        "neatGlamorousLevel",
                        REQUIRED_MESSAGE
                );
                valid = false;
            } else if (!isSliderLevel(neatGlamorousLevel)) {
                addViolation(
                        context,
                        "neatGlamorousLevel",
                        INVALID_VALUE_MESSAGE
                );
                valid = false;
            }
        }

        List<String> styleTags = requestContext.styleTags();

        if (styleTags == null) {
            if (!hasSliderLevels) {
                addViolation(
                        context,
                        "casualFormalLevel",
                        REQUIRED_MESSAGE
                );
                addViolation(
                        context,
                        "neatGlamorousLevel",
                        REQUIRED_MESSAGE
                );
                valid = false;
            }
        } else if (styleTags.isEmpty()
                || styleTags.size() > STYLE_TAGS.size()) {
            addViolation(
                    context,
                    "styleTags",
                    STYLE_TAG_SIZE_MESSAGE
            );
            valid = false;
        } else {
            Set<String> normalized = new HashSet<>();

            for (String styleTag : styleTags) {
                if (!hasText(styleTag)
                        || !STYLE_TAGS.contains(styleTag.trim())) {
                    addViolation(
                            context,
                            "styleTags",
                            INVALID_VALUE_MESSAGE
                    );
                    valid = false;
                    break;
                }

                if (!normalized.add(styleTag.trim())) {
                    addViolation(
                            context,
                            "styleTags",
                            DUPLICATE_MESSAGE
                    );
                    valid = false;
                    break;
                }
            }
        }

        String weatherCondition =
                requestContext.weatherCondition();

        if (weatherCondition != null) {
            if (!hasText(weatherCondition)
                    || !WEATHER_CONDITIONS.contains(
                    weatherCondition.trim()
            )) {
                addViolation(
                        context,
                        "weatherCondition",
                        INVALID_VALUE_MESSAGE
                );
                valid = false;
            }
        }

        if (requestContext.prioritizeOwnedItems() == null) {
            addViolation(
                    context,
                    "prioritizeOwnedItems",
                    REQUIRED_MESSAGE
            );
            valid = false;
        }

        if (!hasText(requestContext.language())) {
            addViolation(
                    context,
                    "language",
                    REQUIRED_MESSAGE
            );
            valid = false;
        } else if (!"ko".equals(requestContext.language().trim())) {
            addViolation(
                    context,
                    "language",
                    INVALID_VALUE_MESSAGE
            );
            valid = false;
        }

        return valid;
    }

    private boolean validateNoStylePlanFields(
            AiJobCreateRequest.Context requestContext,
            ConstraintValidatorContext context,
            boolean valid
    ) {
        if (requestContext.occasion() != null) {
            addViolation(context, "occasion", NOT_ALLOWED_MESSAGE);
            valid = false;
        }
        if (requestContext.casualFormalLevel() != null) {
            addViolation(
                    context,
                    "casualFormalLevel",
                    NOT_ALLOWED_MESSAGE
            );
            valid = false;
        }
        if (requestContext.neatGlamorousLevel() != null) {
            addViolation(
                    context,
                    "neatGlamorousLevel",
                    NOT_ALLOWED_MESSAGE
            );
            valid = false;
        }
        if (requestContext.styleTags() != null) {
            addViolation(context, "styleTags", NOT_ALLOWED_MESSAGE);
            valid = false;
        }
        if (requestContext.weatherCondition() != null) {
            addViolation(context, "weatherCondition", NOT_ALLOWED_MESSAGE);
            valid = false;
        }
        if (requestContext.prioritizeOwnedItems() != null) {
            addViolation(context, "prioritizeOwnedItems", NOT_ALLOWED_MESSAGE);
            valid = false;
        }
        if (requestContext.language() != null) {
            addViolation(context, "language", NOT_ALLOWED_MESSAGE);
            valid = false;
        }
        return valid;
    }

    private void addViolation(
            ConstraintValidatorContext context,
            String field,
            String message
    ) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        message
                )
                .addPropertyNode("context")
                .addPropertyNode(field)
                .addConstraintViolation();
    }

    private boolean isSliderLevel(Integer value) {
        return value != null && value >= 1 && value <= 10;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
