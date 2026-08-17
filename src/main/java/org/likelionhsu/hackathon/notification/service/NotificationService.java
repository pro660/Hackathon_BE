package org.likelionhsu.hackathon.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.notification.domain.NotificationData;
import org.likelionhsu.hackathon.notification.dto.NotificationReadUpdateRequest;
import org.likelionhsu.hackathon.notification.dto.NotificationResponse;
import org.likelionhsu.hackathon.notification.repository.NotificationRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserItemImageRepository userItemImageRepository;
    private final Clock clock;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserItemImageRepository userItemImageRepository,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.userItemImageRepository = userItemImageRepository;
        this.clock = clock;
    }

    public PageResponse<NotificationResponse> getNotifications(
            Long userId,
            int page,
            int size,
            boolean ascending
    ) {
        long total =
                notificationRepository.countByUserId(userId);

        int totalPages = total == 0
                ? 0
                : (int) Math.ceil(
                        (double) total / size
                );

        long offsetLong = (long) page * size;

        if (offsetLong > Integer.MAX_VALUE) {
            return new PageResponse<>(
                    List.of(),
                    page,
                    size,
                    total,
                    totalPages,
                    false,
                    page > 0
            );
        }

        List<NotificationData> rows =
                notificationRepository.findByUserId(
                        userId,
                        size,
                        (int) offsetLong,
                        ascending
                );

        Map<Long, String> images =
                loadImages(userId, rows);

        List<NotificationResponse> items =
                rows.stream()
                        .map(row -> toResponse(
                                row,
                                images.get(row.userItemId())
                        ))
                        .toList();

        boolean hasNext =
                ((long) page + 1L) * size < total;

        return new PageResponse<>(
                items,
                page,
                size,
                total,
                totalPages,
                hasNext,
                page > 0
        );
    }

    @Transactional
    public NotificationResponse updateRead(
            Long userId,
            Long notificationId,
            NotificationReadUpdateRequest request
    ) {
        NotificationData current =
                notificationRepository
                        .findByIdAndUserId(
                                notificationId,
                                userId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.NOTIFICATION_NOT_FOUND
                                )
                        );

        Instant now = clock.instant();
        Instant readAt = request.read() ? now : null;

        int updated = notificationRepository.updateReadAt(
                notificationId,
                userId,
                readAt,
                now
        );

        if (updated != 1) {
            throw new BusinessException(
                    ErrorCode.NOTIFICATION_NOT_FOUND
            );
        }

        String imageUrl = null;

        if (current.userItemId() != null) {
            imageUrl = userItemImageRepository
                    .findPrimaryImageUrls(
                            userId,
                            List.of(current.userItemId())
                    )
                    .get(current.userItemId());
        }

        return toResponse(
                current.withReadAt(readAt),
                imageUrl
        );
    }

    private Map<Long, String> loadImages(
            Long userId,
            List<NotificationData> rows
    ) {
        List<Long> itemIds = rows
                .stream()
                .map(NotificationData::userItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (itemIds.isEmpty()) {
            return Map.of();
        }

        return userItemImageRepository.findPrimaryImageUrls(
                userId,
                itemIds
        );
    }

    private NotificationResponse toResponse(
            NotificationData row,
            String imageUrl
    ) {
        return new NotificationResponse(
                String.valueOf(row.id()),
                row.type(),
                row.title(),
                row.message(),
                row.userItemId() == null
                        ? null
                        : String.valueOf(row.userItemId()),
                row.itemName(),
                imageUrl,
                row.scheduledDate(),
                row.routineTypes(),
                row.readAt() != null,
                row.createdAt()
        );
    }
}
