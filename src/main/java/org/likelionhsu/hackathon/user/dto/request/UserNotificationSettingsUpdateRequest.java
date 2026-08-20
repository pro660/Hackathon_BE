package org.likelionhsu.hackathon.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserNotificationSettingsUpdateRequest(
        @NotNull(message = "필수 입력값입니다.")
        Boolean careReminderEnabled,

        @NotNull(message = "필수 입력값입니다.")
        Boolean recommendationUpdateEnabled,

        @NotNull(message = "필수 입력값입니다.")
        Boolean marketingPushEnabled,

        @NotNull(message = "필수 입력값입니다.")
        Boolean emailMarketingEnabled
) {
}
