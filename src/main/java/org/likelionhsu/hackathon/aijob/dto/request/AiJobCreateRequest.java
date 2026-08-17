package org.likelionhsu.hackathon.aijob.dto.request;

import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.validation.ValidAiJobContext;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@ValidAiJobContext
public record AiJobCreateRequest(
        @NotNull(message = "필수 입력값입니다.")
        AiJobType type,

        @NotNull(message = "필수 입력값입니다.")
        @Valid
        Context context
) {

    public record Context(
            String productId,
            String imageAssetId
    ) {

        public Context(String productId) {
            this(productId, null);
        }
    }
}
