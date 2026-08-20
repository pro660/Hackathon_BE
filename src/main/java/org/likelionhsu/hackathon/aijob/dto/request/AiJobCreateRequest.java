package org.likelionhsu.hackathon.aijob.dto.request;

import java.util.List;

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
            String imageAssetId,
            String occasion,
            Integer casualFormalLevel,
            Integer neatGlamorousLevel,
            List<String> styleTags,
            String weatherCondition,
            Boolean prioritizeOwnedItems,
            String language
    ) {

        public Context(String productId) {
            this(
                    productId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        public Context(
                String productId,
                String imageAssetId
        ) {
            this(
                    productId,
                    imageAssetId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        public Context(
                String productId,
                String imageAssetId,
                String occasion,
                List<String> styleTags,
                String weatherCondition,
                Boolean prioritizeOwnedItems,
                String language
        ) {
            this(
                    productId,
                    imageAssetId,
                    occasion,
                    null,
                    null,
                    styleTags,
                    weatherCondition,
                    prioritizeOwnedItems,
                    language
            );
        }
    }
}
