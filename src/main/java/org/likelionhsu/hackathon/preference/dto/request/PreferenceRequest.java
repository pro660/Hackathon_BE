package org.likelionhsu.hackathon.preference.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PreferenceRequest(
        @NotNull(message = "필수 입력값입니다.")
        @Size(
                min = 1,
                max = 3,
                message = "1개 이상 3개 이하로 선택해 주세요."
        )
        List<String> preferredColors,

        @NotNull(message = "필수 입력값입니다.")
        @Size(
                min = 1,
                max = 3,
                message = "1개 이상 3개 이하로 선택해 주세요."
        )
        List<String> preferredCategories,

        @NotNull(message = "필수 입력값입니다.")
        @Size(
                min = 1,
                max = 2,
                message = "1개 이상 2개 이하로 선택해 주세요."
        )
        List<String> preferredStyleTags
) {
}