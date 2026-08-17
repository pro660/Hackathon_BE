package org.likelionhsu.hackathon.notification.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.notification.domain.NotificationType;

public record NotificationResponse(
        String notificationId,
        NotificationType type,
        String title,
        String message,
        String myItemId,
        String itemName,
        String imageUrl,
        LocalDate scheduledDate,
        List<CareRoutineType> routineTypes,
        boolean read,
        Instant createdAt
) {
}
