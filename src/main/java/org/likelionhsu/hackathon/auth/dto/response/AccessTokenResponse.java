package org.likelionhsu.hackathon.auth.dto.response;

public record AccessTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
