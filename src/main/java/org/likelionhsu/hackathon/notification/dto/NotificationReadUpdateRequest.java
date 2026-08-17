package org.likelionhsu.hackathon.notification.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationReadUpdateRequest(
        @NotNull(message = "필수 입력값입니다.")
        Boolean read
) {
}
