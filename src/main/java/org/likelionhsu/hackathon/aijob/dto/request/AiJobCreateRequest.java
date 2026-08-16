package org.likelionhsu.hackathon.aijob.dto.request;

import org.likelionhsu.hackathon.aijob.domain.AiJobType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiJobCreateRequest(
        @NotNull(message = "필수 입력값입니다.")
        AiJobType type,

        @NotNull(message = "필수 입력값입니다.")
        @Valid
        Context context
) {

    public record Context(
            @NotBlank(message = "필수 입력값입니다.")
            String productId
    ) {
    }
}
