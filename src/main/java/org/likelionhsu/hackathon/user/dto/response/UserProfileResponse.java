package org.likelionhsu.hackathon.user.dto.response;

import java.util.List;

import org.likelionhsu.hackathon.auth.domain.Gender;

public record UserProfileResponse(
        String userId,
        String nickname,
        Gender gender,
        List<String> authenticationMethods
) {
    public UserProfileResponse {
        authenticationMethods = List.copyOf(
                authenticationMethods
        );
    }
}
