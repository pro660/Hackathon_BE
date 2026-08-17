package org.likelionhsu.hackathon.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.notification.domain.NotificationData;
import org.likelionhsu.hackathon.notification.domain.NotificationType;
import org.likelionhsu.hackathon.notification.dto.NotificationResponse;
import org.likelionhsu.hackathon.notification.repository.NotificationRepository;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    UserItemImageRepository userItemImageRepository;

    @Test
    void notificationListIncludesCurrentItemImage() {
        Instant createdAt =
                Instant.parse("2026-11-10T00:30:00Z");

        when(notificationRepository.countByUserId(1L))
                .thenReturn(1L);
        when(notificationRepository.findByUserId(
                1L,
                20,
                0,
                false
        ))
                .thenReturn(
                        List.of(
                                new NotificationData(
                                        100L,
                                        1L,
                                        NotificationType.CARE_REMINDER,
                                        "가죽 클리닝 시기예요",
                                        "내 가방의 권장 관리 시기가 되었어요.",
                                        10L,
                                        "내 가방",
                                        LocalDate.of(2026, 11, 10),
                                        List.of(
                                                CareRoutineType.CLEANING
                                        ),
                                        null,
                                        createdAt
                                )
                        )
                );
        when(userItemImageRepository.findPrimaryImageUrls(
                1L,
                List.of(10L)
        ))
                .thenReturn(
                        Map.of(
                                10L,
                                "https://example.com/item.webp"
                        )
                );

        NotificationService service =
                new NotificationService(
                        notificationRepository,
                        userItemImageRepository,
                        Clock.fixed(
                                createdAt,
                                ZoneOffset.UTC
                        )
                );

        PageResponse<NotificationResponse> response =
                service.getNotifications(
                        1L,
                        0,
                        20,
                        false
                );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().notificationId())
                .isEqualTo("100");
        assertThat(response.items().getFirst().imageUrl())
                .isEqualTo("https://example.com/item.webp");
        assertThat(response.items().getFirst().read())
                .isFalse();
    }
}
