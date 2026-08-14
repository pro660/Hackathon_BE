package org.likelionhsu.hackathon.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Pattern(
                regexp = "^[a-z0-9_]{4,20}$",
                message = "로그인 아이디는 영문 소문자, 숫자, 밑줄로 4~20자여야 합니다."
        )
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 64, message = "비밀번호는 64자 이하여야 합니다.")
        String password
) {
}
