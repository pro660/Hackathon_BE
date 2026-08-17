package org.likelionhsu.hackathon.aijob.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class AiJobContextValidatorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory =
                Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void purchaseUtilityAcceptsOnlyProductId() {
        AiJobCreateRequest request =
                new AiJobCreateRequest(
                        AiJobType.PURCHASE_UTILITY,
                        new AiJobCreateRequest.Context(
                                "123",
                                null
                        )
                );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void itemAnalysisAcceptsOnlyImageAssetId() {
        AiJobCreateRequest request =
                new AiJobCreateRequest(
                        AiJobType.ITEM_ANALYSIS,
                        new AiJobCreateRequest.Context(
                                null,
                                "51"
                        )
                );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void stylePlanAcceptsFinalV03Context() {
        AiJobCreateRequest request =
                new AiJobCreateRequest(
                        AiJobType.STYLE_PLAN,
                        new AiJobCreateRequest.Context(
                                null,
                                null,
                                "DATE",
                                List.of("NEAT", "GLAMOROUS"),
                                "INDOOR",
                                true,
                                "ko"
                        )
                );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void stylePlanAllowsMissingWeatherCondition() {
        AiJobCreateRequest request =
                new AiJobCreateRequest(
                        AiJobType.STYLE_PLAN,
                        new AiJobCreateRequest.Context(
                                null,
                                null,
                                "DAILY",
                                List.of("CASUAL"),
                                null,
                                true,
                                "ko"
                        )
                );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void stylePlanRejectsInvalidOccasion() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.STYLE_PLAN,
                                new AiJobCreateRequest.Context(
                                        null,
                                        null,
                                        "WORK",
                                        List.of("NEAT"),
                                        null,
                                        true,
                                        "ko"
                                )
                        )
                );

        assertViolation(
                violations,
                "context.occasion",
                "허용되지 않는 값입니다."
        );
    }

    @Test
    void stylePlanRejectsInvalidStyleTag() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.STYLE_PLAN,
                                new AiJobCreateRequest.Context(
                                        null,
                                        null,
                                        "DATE",
                                        List.of("MINIMAL"),
                                        null,
                                        true,
                                        "ko"
                                )
                        )
                );

        assertViolation(
                violations,
                "context.styleTags",
                "허용되지 않는 값입니다."
        );
    }

    @Test
    void stylePlanRejectsDuplicateStyleTags() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.STYLE_PLAN,
                                new AiJobCreateRequest.Context(
                                        null,
                                        null,
                                        "DATE",
                                        List.of("NEAT", "NEAT"),
                                        null,
                                        true,
                                        "ko"
                                )
                        )
                );

        assertViolation(
                violations,
                "context.styleTags",
                "중복 값을 사용할 수 없습니다."
        );
    }

    @Test
    void stylePlanRejectsInvalidWeatherCondition() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.STYLE_PLAN,
                                new AiJobCreateRequest.Context(
                                        null,
                                        null,
                                        "DATE",
                                        List.of("NEAT"),
                                        "FOGGY",
                                        true,
                                        "ko"
                                )
                        )
                );

        assertViolation(
                violations,
                "context.weatherCondition",
                "허용되지 않는 값입니다."
        );
    }

    @Test
    void stylePlanRequiresPrioritizeOwnedItems() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.STYLE_PLAN,
                                new AiJobCreateRequest.Context(
                                        null,
                                        null,
                                        "DATE",
                                        List.of("NEAT"),
                                        null,
                                        null,
                                        "ko"
                                )
                        )
                );

        assertViolation(
                violations,
                "context.prioritizeOwnedItems",
                "필수 입력값입니다."
        );
    }

    @Test
    void stylePlanRejectsUnsupportedLanguage() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.STYLE_PLAN,
                                new AiJobCreateRequest.Context(
                                        null,
                                        null,
                                        "DATE",
                                        List.of("NEAT"),
                                        null,
                                        true,
                                        "en"
                                )
                        )
                );

        assertViolation(
                violations,
                "context.language",
                "허용되지 않는 값입니다."
        );
    }

    @Test
    void purchaseUtilityRequiresProductId() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.PURCHASE_UTILITY,
                                new AiJobCreateRequest.Context(
                                        " ",
                                        null
                                )
                        )
                );

        assertViolation(
                violations,
                "context.productId",
                "필수 입력값입니다."
        );
    }

    @Test
    void itemAnalysisRequiresImageAssetId() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.ITEM_ANALYSIS,
                                new AiJobCreateRequest.Context(
                                        null,
                                        " "
                                )
                        )
                );

        assertViolation(
                violations,
                "context.imageAssetId",
                "필수 입력값입니다."
        );
    }

    @Test
    void itemAnalysisRejectsPurchaseUtilityField() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.ITEM_ANALYSIS,
                                new AiJobCreateRequest.Context(
                                        "123",
                                        "51"
                                )
                        )
                );

        assertViolation(
                violations,
                "context.productId",
                "해당 AI 작업 타입에서는 사용할 수 없습니다."
        );
    }

    @Test
    void purchaseUtilityRejectsItemAnalysisField() {
        Set<ConstraintViolation<AiJobCreateRequest>> violations =
                validator.validate(
                        new AiJobCreateRequest(
                                AiJobType.PURCHASE_UTILITY,
                                new AiJobCreateRequest.Context(
                                        "123",
                                        "51"
                                )
                        )
                );

        assertViolation(
                violations,
                "context.imageAssetId",
                "해당 AI 작업 타입에서는 사용할 수 없습니다."
        );
    }

    private void assertViolation(
            Set<ConstraintViolation<AiJobCreateRequest>> violations,
            String field,
            String message
    ) {
        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation
                                    .getPropertyPath()
                                    .toString()
                    ).isEqualTo(field);

                    assertThat(violation.getMessage())
                            .isEqualTo(message);
                });
    }
}
