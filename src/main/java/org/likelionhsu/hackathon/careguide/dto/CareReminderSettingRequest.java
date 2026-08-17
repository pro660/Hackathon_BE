package org.likelionhsu.hackathon.careguide.dto;

import jakarta.validation.constraints.NotNull;

public record CareReminderSettingRequest(
        @NotNull(message = "필수 입력값입니다.")
        Boolean enabled
) {
}
