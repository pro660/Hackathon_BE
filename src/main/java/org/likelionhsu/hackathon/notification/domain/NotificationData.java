package org.likelionhsu.hackathon.notification.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;

public record NotificationData(
        Long id,
        Long userId,
        NotificationType type,
        String title,
        String message,
        Long userItemId,
        String itemName,
        LocalDate scheduledDate,
        List<CareRoutineType> routineTypes,
        Instant readAt,
        Instant createdAt
) {

    public NotificationData withReadAt(Instant value) {
        return new NotificationData(
                id,
                userId,
                type,
                title,
                message,
                userItemId,
                itemName,
                scheduledDate,
                routineTypes,
                value,
                createdAt
        );
    }
}
