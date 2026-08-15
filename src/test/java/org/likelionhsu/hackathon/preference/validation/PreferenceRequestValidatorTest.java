package org.likelionhsu.hackathon.preference.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.preference.dto.request.PreferenceRequest;
import org.likelionhsu.hackathon.preference.entity.PreferenceStyleTag;
import org.likelionhsu.hackathon.preference.validation.PreferenceRequestValidator.ValidatedPreferenceRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PreferenceRequestValidatorTest {

    private Validator beanValidator;
    private PreferenceRequestValidator requestValidator;

    @BeforeEach
    void setUp() {
        beanValidator =
                Validation
                        .buildDefaultValidatorFactory()
                        .getValidator();

        requestValidator =
                new PreferenceRequestValidator();
    }

    @Test
    void jakartaValidationRejectsNullFields() {
        assertBeanValidationFailure(
                new PreferenceRequest(
                        null,
                        List.of("BAG"),
                        List.of("CASUAL")
                ),
                "preferredColors",
                "필수 입력값입니다."
        );

        assertBeanValidationFailure(
                new PreferenceRequest(
                        List.of("BLACK"),
                        null,
                        List.of("CASUAL")
                ),
                "preferredCategories",
                "필수 입력값입니다."
        );

        assertBeanValidationFailure(
                new PreferenceRequest(
                        List.of("BLACK"),
                        List.of("BAG"),
                        null
                ),
                "preferredStyleTags",
                "필수 입력값입니다."
        );
    }

    @Test
    void jakartaValidationRejectsInvalidColorCount() {
        assertBeanValidationFailure(
                new PreferenceRequest(
                        List.of(),
                        List.of("BAG"),
                        List.of("CASUAL")
                ),
                "preferredColors",
                "1개 이상 3개 이하로 선택해 주세요."
        );

        assertBeanValidationFailure(
                new PreferenceRequest(
                        List.of(
                                "BLACK",
                                "WHITE",
                                "GRAY",
                                "BROWN"
                        ),
                        List.of("BAG"),
                        List.of("CASUAL")
                ),
                "preferredColors",
                "1개 이상 3개 이하로 선택해 주세요."
        );
    }

    @Test
    void jakartaValidationRejectsInvalidCategoryCount() {
        assertBeanValidationFailure(
                new PreferenceRequest(
                        List.of("BLACK"),
                        List.of(),
                        List.of("CASUAL")
                ),
                "preferredCategories",
                "1개 이상 3개 이하로 선택해 주세요."
        );

        assertBeanValidationFailure(
                new PreferenceRequest(
                        List.of("BLACK"),
                        List.of(
                                "BAG",
                                "SHOES",
                                "CLOTHING",
                                "LEATHER_GOODS"
                        ),
                        List.of("CASUAL")
                ),
                "preferredCategories",
                "1개 이상 3개 이하로 선택해 주세요."
        );
    }

    @Test
    void jakartaValidationRejectsInvalidStyleTagCount() {
        assertBeanValidationFailure(
                new PreferenceRequest(
                        List.of("BLACK"),
                        List.of("BAG"),
                        List.of()
                ),
                "preferredStyleTags",
                "1개 이상 2개 이하로 선택해 주세요."
        );

        assertBeanValidationFailure(
                new PreferenceRequest(
                        List.of("BLACK"),
                        List.of("BAG"),
                        List.of(
                                "CASUAL",
                                "FORMAL",
                                "NEAT"
                        )
                ),
                "preferredStyleTags",
                "1개 이상 2개 이하로 선택해 주세요."
        );
    }

    @Test
    void customValidationRejectsNullElement() {
        PreferenceRequest request =
                new PreferenceRequest(
                        Arrays.asList(
                                (String) null
                        ),
                        List.of("BAG"),
                        List.of("CASUAL")
                );

        assertCustomValidationFailure(
                request,
                "preferredColors",
                "빈 값은 사용할 수 없습니다."
        );
    }

    @Test
    void customValidationRejectsEmptyElement() {
        assertCustomValidationFailure(
                new PreferenceRequest(
                        List.of(""),
                        List.of("BAG"),
                        List.of("CASUAL")
                ),
                "preferredColors",
                "빈 값은 사용할 수 없습니다."
        );
    }

    @Test
    void customValidationRejectsBlankElement() {
        assertCustomValidationFailure(
                new PreferenceRequest(
                        List.of("   "),
                        List.of("BAG"),
                        List.of("CASUAL")
                ),
                "preferredColors",
                "빈 값은 사용할 수 없습니다."
        );
    }

    @Test
    void customValidationRejectsDuplicateValue() {
        assertCustomValidationFailure(
                new PreferenceRequest(
                        List.of("BLACK"),
                        List.of(
                                "BAG",
                                "BAG"
                        ),
                        List.of("CASUAL")
                ),
                "preferredCategories",
                "중복된 값은 선택할 수 없습니다."
        );
    }

    @Test
    void customValidationRejectsWrongCase() {
        assertCustomValidationFailure(
                new PreferenceRequest(
                        List.of("black"),
                        List.of("BAG"),
                        List.of("CASUAL")
                ),
                "preferredColors",
                "허용되지 않은 값이 포함되어 있습니다."
        );
    }

    @Test
    void customValidationRejectsPaddedWhitespace() {
        assertCustomValidationFailure(
                new PreferenceRequest(
                        List.of(" BLACK "),
                        List.of("BAG"),
                        List.of("CASUAL")
                ),
                "preferredColors",
                "허용되지 않은 값이 포함되어 있습니다."
        );
    }

    @Test
    void customValidationRejectsUnknownValue() {
        assertCustomValidationFailure(
                new PreferenceRequest(
                        List.of("BLACK"),
                        List.of("UNKNOWN"),
                        List.of("CASUAL")
                ),
                "preferredCategories",
                "허용되지 않은 값이 포함되어 있습니다."
        );
    }

    @Test
    void allColorCodesAreAccepted() {
        for (ColorGroup color : ColorGroup.values()) {
            requestValidator.validate(
                    new PreferenceRequest(
                            List.of(color.name()),
                            List.of("BAG"),
                            List.of("CASUAL")
                    )
            );
        }
    }

    @Test
    void allCategoryCodesAreAccepted() {
        for (ItemCategory category
                : ItemCategory.values()) {

            requestValidator.validate(
                    new PreferenceRequest(
                            List.of("BLACK"),
                            List.of(category.name()),
                            List.of("CASUAL")
                    )
            );
        }
    }

    @Test
    void allStyleTagCodesAreAccepted() {
        for (PreferenceStyleTag styleTag
                : PreferenceStyleTag.values()) {

            requestValidator.validate(
                    new PreferenceRequest(
                            List.of("BLACK"),
                            List.of("BAG"),
                            List.of(styleTag.name())
                    )
            );
        }
    }

    @Test
    void validRequestIsConvertedAndSortedByEnumCode() {
        PreferenceRequest request =
                new PreferenceRequest(
                        List.of(
                                "WHITE",
                                "BLACK",
                                "BEIGE"
                        ),
                        List.of(
                                "SHOES",
                                "BAG",
                                "CLOTHING"
                        ),
                        List.of(
                                "NEAT",
                                "CASUAL"
                        )
                );

        ValidatedPreferenceRequest validated =
                requestValidator.validate(request);

        assertThat(validated.preferredColors())
                .containsExactly(
                        ColorGroup.BEIGE,
                        ColorGroup.BLACK,
                        ColorGroup.WHITE
                );

        assertThat(validated.preferredCategories())
                .containsExactly(
                        ItemCategory.BAG,
                        ItemCategory.CLOTHING,
                        ItemCategory.SHOES
                );

        assertThat(validated.preferredStyleTags())
                .containsExactly(
                        PreferenceStyleTag.CASUAL,
                        PreferenceStyleTag.NEAT
                );
    }

    private void assertBeanValidationFailure(
            PreferenceRequest request,
            String expectedField,
            String expectedReason
    ) {
        Set<ConstraintViolation<PreferenceRequest>>
                violations =
                beanValidator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(
                            violation
                                    .getPropertyPath()
                                    .toString()
                    ).isEqualTo(expectedField);

                    assertThat(
                            violation.getMessage()
                    ).isEqualTo(expectedReason);
                });
    }

    private void assertCustomValidationFailure(
            PreferenceRequest request,
            String expectedField,
            String expectedReason
    ) {
        try {
            requestValidator.validate(request);
        } catch (RequestValidationException exception) {
            assertThat(exception.getField())
                    .isEqualTo(expectedField);

            assertThat(exception.getReason())
                    .isEqualTo(expectedReason);

            return;
        }

        throw new AssertionError(
                "RequestValidationException이 발생해야 합니다."
        );
    }
}