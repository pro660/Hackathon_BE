package org.likelionhsu.hackathon.careguide.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.careguide.domain.CareReminderSetting;
import org.likelionhsu.hackathon.careguide.dto.CareReminderSettingRequest;
import org.likelionhsu.hackathon.careguide.dto.CareReminderSettingResponse;
import org.likelionhsu.hackathon.careguide.repository.CareReminderSettingRepository;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CareReminderSettingServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ITEM_ID = 10L;
    private static final Instant NOW =
            Instant.parse("2026-08-18T00:00:00Z");

    @Mock
    UserItemRepository userItemRepository;

    @Mock
    CareReminderSettingRepository settingRepository;

    @Mock
    CareGuideService careGuideService;

    @Mock
    UserItem item;

    @Test
    void repeatedEnableKeepsOriginalEnabledAt() {
        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        ITEM_ID,
                        USER_ID
                ))
                .thenReturn(Optional.of(item));

        when(careGuideService.resolve(item))
                .thenReturn(
                        new CareGuideService.ResolvedPolicy(
                                null,
                                true,
                                true,
                                true,
                                true,
                                null
                        )
                );

        Instant firstEnabledAt =
                Instant.parse("2026-08-17T00:00:00Z");

        when(settingRepository.findByUserItemId(ITEM_ID))
                .thenReturn(
                        Optional.of(
                                new CareReminderSetting(
                                        ITEM_ID,
                                        true,
                                        firstEnabledAt
                                )
                        )
                );

        CareReminderSettingService service =
                new CareReminderSettingService(
                        userItemRepository,
                        settingRepository,
                        careGuideService,
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        CareReminderSettingResponse response =
                service.updateSetting(
                        USER_ID,
                        ITEM_ID,
                        new CareReminderSettingRequest(true)
                );

        assertThat(response.enabled()).isTrue();
        assertThat(response.enabledAt())
                .isEqualTo(firstEnabledAt);

        verify(settingRepository).upsert(
                ITEM_ID,
                true,
                firstEnabledAt,
                NOW
        );
    }
}
