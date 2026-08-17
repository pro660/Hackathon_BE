package org.likelionhsu.hackathon.styleplan.dto.response;

public record StylePlanCreateResponse(
        String stylePlanId
) {

    public static StylePlanCreateResponse from(
            Long stylePlanId
    ) {
        return new StylePlanCreateResponse(
                String.valueOf(stylePlanId)
        );
    }
}
