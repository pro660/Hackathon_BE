package org.likelionhsu.hackathon.auth.dto.request;

import org.likelionhsu.hackathon.auth.domain.EmailVerificationPurpose;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmailVerificationRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식을 확인해 주세요.")
        @Size(max = 320, message = "이메일은 320자 이하여야 합니다.")
        String email,

        @NotNull(message = "인증 목적은 필수입니다.")
        EmailVerificationPurpose purpose
) {
}
