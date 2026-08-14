package org.likelionhsu.hackathon.auth.dto.request;

import org.likelionhsu.hackathon.auth.domain.TermsType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TermsAgreementRequest(
        @NotNull(message = "약관 종류는 필수입니다.")
        TermsType termsType,

        @NotBlank(message = "약관 버전은 필수입니다.")
        @Size(max = 30, message = "약관 버전은 30자 이하여야 합니다.")
        String termsVersion,

        boolean agreed
) {
}
