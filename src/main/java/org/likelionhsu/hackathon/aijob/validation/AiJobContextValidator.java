package org.likelionhsu.hackathon.aijob.validation;

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
            case STYLE_PLAN -> true;
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

        return valid;
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
