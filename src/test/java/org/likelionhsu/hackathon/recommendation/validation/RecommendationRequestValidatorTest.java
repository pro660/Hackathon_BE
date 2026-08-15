package org.likelionhsu.hackathon.recommendation.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.recommendation.dto.request.RecommendationRequest;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationFeature;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationOccasion;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationSeason;

class RecommendationRequestValidatorTest {

    private final RecommendationRequestValidator validator =
            new RecommendationRequestValidator();

    @Test
    void validatesAndNormalizesRequest() {
        var validated = validator.validate(
                new RecommendationRequest(
                        "DATE",
                        "AUTUMN",
                        List.of("MULTIWAY", "COMPACT"),
                        "BAG",
                        null
                )
        );

        assertThat(validated.occasion())
                .isEqualTo(RecommendationOccasion.DATE);
        assertThat(validated.season())
                .isEqualTo(RecommendationSeason.AUTUMN);
        assertThat(validated.preferredFeatures())
                .containsExactly(
                        RecommendationFeature.COMPACT,
                        RecommendationFeature.MULTIWAY
                );
        assertThat(validated.category())
                .isEqualTo(ItemCategory.BAG);
        assertThat(validated.limit()).isEqualTo(3);
    }

    @Test
    void rejectsLowercaseOccasionWithoutCorrection() {
        assertThatThrownBy(() -> validator.validate(
                new RecommendationRequest(
                        "date",
                        "AUTUMN",
                        List.of("COMPACT"),
                        null,
                        3
                )
        ))
                .isInstanceOf(RequestValidationException.class)
                .satisfies(exception -> {
                    RequestValidationException validation =
                            (RequestValidationException) exception;
                    assertThat(validation.getField())
                            .isEqualTo("occasion");
                });
    }

    @Test
    void rejectsAllSeasonAsRequestSeason() {
        assertThatThrownBy(() -> validator.validate(
                new RecommendationRequest(
                        "DATE",
                        "ALL_SEASON",
                        List.of("COMPACT"),
                        null,
                        3
                )
        )).isInstanceOf(RequestValidationException.class);
    }

    @Test
    void rejectsDuplicateFeatures() {
        assertThatThrownBy(() -> validator.validate(
                new RecommendationRequest(
                        "DATE",
                        "AUTUMN",
                        List.of("COMPACT", "COMPACT"),
                        null,
                        3
                )
        ))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("중복된 값은 선택할 수 없습니다.");
    }

    @Test
    void rejectsLimitOutsideOneToThree() {
        assertThatThrownBy(() -> validator.validate(
                new RecommendationRequest(
                        "DATE",
                        "AUTUMN",
                        List.of("COMPACT"),
                        null,
                        4
                )
        ))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("1 이상 3 이하로 입력해 주세요.");
    }
    @Test
    void rejectsExplicitNullOptionalValues() {
        RecommendationRequest request = new RecommendationRequest();
        request.setOccasion("DATE");
        request.setSeason("AUTUMN");
        request.setPreferredFeatures(List.of("COMPACT"));
        request.setCategory(null);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("null은 사용할 수 없습니다.");

        RecommendationRequest second = new RecommendationRequest();
        second.setOccasion("DATE");
        second.setSeason("AUTUMN");
        second.setPreferredFeatures(List.of("COMPACT"));
        second.setLimit(null);

        assertThatThrownBy(() -> validator.validate(second))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("null은 사용할 수 없습니다.");
    }

}
