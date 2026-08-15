package org.likelionhsu.hackathon.auth.dto.response;

public record EmailVerificationResponse(
        long expiresInSeconds,
        long resendAvailableInSeconds
) {
}
