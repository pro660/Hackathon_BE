package org.likelionhsu.hackathon.careguide.domain;

import java.time.Instant;

public record CareReminderSetting(
        Long userItemId,
        boolean enabled,
        Instant enabledAt
) {
}
