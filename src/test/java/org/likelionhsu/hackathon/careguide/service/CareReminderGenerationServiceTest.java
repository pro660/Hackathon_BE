package org.likelionhsu.hackathon.careguide.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.careguide.domain.CareIntervalUnit;
import org.likelionhsu.hackathon.careguide.domain.CareReminderSetting;
import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicy;
import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicyRegistry;
import org.likelionhsu.hackathon.careguide.repository.CareReminderSettingRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;
import org.likelionhsu.hackathon.notification.repository.NotificationRepository;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CareReminderGenerationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ITEM_ID = 10L;
    private static final Instant NOW =
            Instant.parse("2026-11-09T15:30:00Z");

    @Mock
    CareReminderSettingRepository settingRepository;

    @Mock
    UserItemRepository userItemRepository;

    @Mock
    MaterialCarePolicyRegistry policyRegistry;

    @Mock
    NotificationRepository notificationRepository;

    @Test
    void dueCleaningCreatesOneCareReminderRequest() {
        UserItem item = item();

        when(settingRepository.findAllEnabled())
                .thenReturn(
                        List.of(
                                new CareReminderSetting(
                                        ITEM_ID,
                                        true,
                                        Instant.parse(
                                                "2026-08-18T00:00:00Z"
                                        )
                                )
                        )
                );
        when(userItemRepository.findById(ITEM_ID))
                .thenReturn(Optional.of(item));
        when(policyRegistry.get(MaterialGroup.LEATHER))
                .thenReturn(leatherPolicy());

        service().generateForToday();

        verify(notificationRepository)
                .insertCareReminderIfAbsent(
                        USER_ID,
                        "가죽 클리닝 시기예요",
                        "내 가방의 권장 관리 시기가 되었어요.",
                        ITEM_ID,
                        "내 가방",
                        LocalDate.of(2026, 11, 10),
                        List.of(CareRoutineType.CLEANING),
                        "CARE_REMINDER:10:2026-11-10",
                        NOW
                );
    }

    @Test
    void futureEnabledAtDoesNotBackfillOrGenerate() {
        when(settingRepository.findAllEnabled())
                .thenReturn(
                        List.of(
                                new CareReminderSetting(
                                        ITEM_ID,
                                        true,
                                        Instant.parse(
                                                "2026-11-11T00:00:00Z"
                                        )
                                )
                        )
                );

        service().generateForToday();

        verify(userItemRepository, never()).findById(ITEM_ID);
        verify(notificationRepository, never())
                .insertCareReminderIfAbsent(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    private CareReminderGenerationService service() {
        return new CareReminderGenerationService(
                settingRepository,
                userItemRepository,
                policyRegistry,
                new CareScheduleCalculator(),
                notificationRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private UserItem item() {
        User user = User.local(
                "reminder@example.com",
                "알림사용자",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);

        UserItem item = UserItem.create(
                user,
                null,
                "MCM",
                "내 가방",
                ItemCategory.BAG,
                ColorGroup.BLACK,
                MaterialGroup.LEATHER,
                MaterialSource.USER_CONFIRMED,
                LocalDate.of(2026, 8, 10),
                null,
                null,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(item, "id", ITEM_ID);
        return item;
    }

    private MaterialCarePolicy leatherPolicy() {
        return new MaterialCarePolicy(
                "가죽",
                "가죽 관리",
                "설명",
                List.of(
                        new MaterialCarePolicy.RoutinePolicy(
                                CareRoutineType.CLEANING,
                                3,
                                CareIntervalUnit.MONTH,
                                "가죽 클리닝",
                                "설명"
                        ),
                        new MaterialCarePolicy.RoutinePolicy(
                                CareRoutineType.CONDITIONING,
                                6,
                                CareIntervalUnit.MONTH,
                                "가죽 컨디셔닝",
                                "설명"
                        )
                ),
                new MaterialCarePolicy.StoragePolicy(
                        List.of("환경"),
                        List.of("습기"),
                        List.of("보관")
                )
        );
    }
}
