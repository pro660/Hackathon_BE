package org.likelionhsu.hackathon.auth.dto.response;

public record LoginIdAvailabilityResponse(
        String loginId,
        boolean available
) {
}
