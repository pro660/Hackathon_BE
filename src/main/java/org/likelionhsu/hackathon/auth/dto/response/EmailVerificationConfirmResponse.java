package org.likelionhsu.hackathon.auth.dto.response;

public record EmailVerificationConfirmResponse(
        String signupToken,
        long expiresInSeconds
) {
}
