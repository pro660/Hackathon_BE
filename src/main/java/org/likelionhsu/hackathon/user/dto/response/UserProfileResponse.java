package org.likelionhsu.hackathon.user.dto.response;

import org.likelionhsu.hackathon.auth.domain.Gender;

public record UserProfileResponse(
        String userId,
        String nickname,
        Gender gender
) {
}
