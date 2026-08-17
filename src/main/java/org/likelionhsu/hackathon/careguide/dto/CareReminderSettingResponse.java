package org.likelionhsu.hackathon.careguide.dto;

import java.time.Instant;

public record CareReminderSettingResponse(
        String myItemId,
        boolean enabled,
        Instant enabledAt
) {
}
