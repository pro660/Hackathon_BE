package org.likelionhsu.hackathon.auth.dto.request;

import java.util.List;

import org.likelionhsu.hackathon.auth.domain.Gender;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "회원가입 인증 토큰은 필수입니다.")
        String signupToken,

        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Pattern(
                regexp = "^[a-z0-9_]{4,20}$",
                message = "로그인 아이디는 영문 소문자, 숫자, 밑줄로 4~20자여야 합니다."
        )
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,64}$",
                message = "비밀번호는 영문과 숫자를 포함한 8~64자여야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm,

        @NotEmpty(message = "약관 동의 정보는 필수입니다.")
        @Valid
        List<TermsAgreementRequest> termsAgreements,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        String nickname,

        @NotNull(message = "성별은 필수입니다.")
        Gender gender
) {
}
