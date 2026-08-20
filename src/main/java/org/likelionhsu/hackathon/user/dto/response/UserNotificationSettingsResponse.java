package org.likelionhsu.hackathon.user.dto.response;

public record UserNotificationSettingsResponse(
        boolean careReminderEnabled,
        boolean recommendationUpdateEnabled,
        boolean marketingPushEnabled,
        boolean emailMarketingEnabled
) {
}
