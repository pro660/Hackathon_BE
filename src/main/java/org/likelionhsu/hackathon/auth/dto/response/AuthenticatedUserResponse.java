package org.likelionhsu.hackathon.auth.dto.response;

import org.likelionhsu.hackathon.auth.domain.Gender;

public record AuthenticatedUserResponse(
        String userId,
        String nickname,
        Gender gender
) {
}
