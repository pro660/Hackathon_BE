package org.likelionhsu.hackathon.auth.dto.request;

import java.util.List;

import org.likelionhsu.hackathon.auth.domain.Gender;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SocialSignupRequest(
        @NotEmpty(message = "약관 동의 정보는 필수입니다.")
        @Valid
        List<TermsAgreementRequest> termsAgreements,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(
                min = 2,
                max = 20,
                message = "닉네임은 2~20자여야 합니다."
        )
        String nickname,

        @NotNull(message = "성별은 필수입니다.")
        Gender gender,

        @Email(message = "알림 이메일 형식이 올바르지 않습니다.")
        @Size(max = 320, message = "알림 이메일은 320자 이하여야 합니다.")
        String notificationEmail
) {
}
