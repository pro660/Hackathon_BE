package org.likelionhsu.hackathon.styleplan.dto.request;

import java.time.Instant;
import java.util.List;

import org.likelionhsu.hackathon.styleplan.domain.StyleItemRole;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanWeatherCondition;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StylePlanCreateRequest(
        @Positive(message = "1 이상이어야 합니다.")
        Long aiJobId,

        @NotBlank(message = "필수 입력값입니다.")
        @Size(
                max = 200,
                message = "200자 이하여야 합니다."
        )
        String title,

        @NotNull(message = "필수 입력값입니다.")
        StylePlanOccasion occasion,

        Instant plannedAt,

        StylePlanWeatherCondition weatherCondition,

        @Size(
                max = 1500,
                message = "1500자 이하여야 합니다."
        )
        String description,

        @NotNull(message = "필수 입력값입니다.")
        StylePlanStatus status,

        @NotNull(message = "필수 입력값입니다.")
        @Size(
                max = 10,
                message = "보유 아이템은 최대 10개까지 선택할 수 있습니다."
        )
        List<@Valid OwnedItem> ownedItems,

        @NotNull(message = "필수 입력값입니다.")
        @Size(
                max = 3,
                message = "추천 상품은 최대 3개까지 선택할 수 있습니다."
        )
        List<@Valid RecommendedProduct> recommendedProducts
) {

    public StylePlanCreateRequest {
        ownedItems = ownedItems == null
                ? null
                : List.copyOf(ownedItems);
        recommendedProducts =
                recommendedProducts == null
                        ? null
                        : List.copyOf(recommendedProducts);
    }

    public record OwnedItem(
            @NotNull(message = "필수 입력값입니다.")
            @Positive(message = "1 이상이어야 합니다.")
            Long myItemId,

            @NotNull(message = "필수 입력값입니다.")
            StyleItemRole role,

            @Min(
                    value = 0,
                    message = "0 이상이어야 합니다."
            )
            int sortOrder
    ) {
    }

    public record RecommendedProduct(
            @NotNull(message = "필수 입력값입니다.")
            @Positive(message = "1 이상이어야 합니다.")
            Long productId,

            @Min(
                    value = 1,
                    message = "1 이상이어야 합니다."
            )
            int rank,

            @Size(
                    max = 1000,
                    message = "1000자 이하여야 합니다."
            )
            String reason
    ) {
    }
}
