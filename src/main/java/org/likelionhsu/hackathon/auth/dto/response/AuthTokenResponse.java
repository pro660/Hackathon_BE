package org.likelionhsu.hackathon.auth.dto.response;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        AuthenticatedUserResponse user
) {
}
