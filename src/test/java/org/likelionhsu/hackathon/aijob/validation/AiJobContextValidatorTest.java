package org.likelionhsu.hackathon.aijob.validation;

import static org.assertj.core.api.Assertions.assertThat;

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
    void purchaseUtilityRequiresProductId() {
        Set<ConstraintViolation<AiJobCreateRequest>>
                violations =
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
        Set<ConstraintViolation<AiJobCreateRequest>>
                violations =
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
        Set<ConstraintViolation<AiJobCreateRequest>>
                violations =
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
        Set<ConstraintViolation<AiJobCreateRequest>>
                violations =
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
            Set<ConstraintViolation<AiJobCreateRequest>>
                    violations,
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
