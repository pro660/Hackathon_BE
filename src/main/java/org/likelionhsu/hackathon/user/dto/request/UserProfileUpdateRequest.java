package org.likelionhsu.hackathon.user.dto.request;

import org.likelionhsu.hackathon.auth.domain.Gender;

public record UserProfileUpdateRequest(
        String nickname,
        Gender gender
) {
}
