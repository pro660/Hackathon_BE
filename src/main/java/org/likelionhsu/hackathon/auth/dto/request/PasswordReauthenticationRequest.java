package org.likelionhsu.hackathon.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordReauthenticationRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
