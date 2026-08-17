package org.likelionhsu.hackathon.careguide.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.careguide.domain.CareReminderSetting;
import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicy;
import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicyRegistry;
import org.likelionhsu.hackathon.careguide.repository.CareReminderSettingRepository;
import org.likelionhsu.hackathon.notification.repository.NotificationRepository;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareReminderGenerationService {

    private static final ZoneId SEOUL =
            ZoneId.of("Asia/Seoul");

    private final CareReminderSettingRepository settingRepository;
    private final UserItemRepository userItemRepository;
    private final MaterialCarePolicyRegistry policyRegistry;
    private final CareScheduleCalculator scheduleCalculator;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public CareReminderGenerationService(
            CareReminderSettingRepository settingRepository,
            UserItemRepository userItemRepository,
            MaterialCarePolicyRegistry policyRegistry,
            CareScheduleCalculator scheduleCalculator,
            NotificationRepository notificationRepository,
            Clock clock
    ) {
        this.settingRepository = settingRepository;
        this.userItemRepository = userItemRepository;
        this.policyRegistry = policyRegistry;
        this.scheduleCalculator = scheduleCalculator;
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Transactional
    public void generateForToday() {
        Instant now = clock.instant();
        LocalDate today =
                LocalDate.now(clock.withZone(SEOUL));

        for (CareReminderSetting setting
                : settingRepository.findAllEnabled()) {
            generateForSetting(
                    setting,
                    today,
                    now
            );
        }
    }

    private void generateForSetting(
            CareReminderSetting setting,
            LocalDate today,
            Instant now
    ) {
        if (!setting.enabled()
                || setting.enabledAt() == null) {
            return;
        }

        LocalDate enabledDate =
                setting.enabledAt()
                        .atZone(SEOUL)
                        .toLocalDate();

        if (today.isBefore(enabledDate)) {
            return;
        }

        UserItem item = userItemRepository
                .findById(setting.userItemId())
                .orElse(null);

        if (item == null
                || item.getDeletedAt() != null
                || item.getUser().getStatus() != UserStatus.ACTIVE
                || item.getMaterial() == null
                || item.getPurchaseDate() == null) {
            return;
        }

        MaterialCarePolicy policy =
                policyRegistry.get(item.getMaterial());

        if (policy.routines().isEmpty()) {
            return;
        }

        CareScheduleCalculator.ScheduleEvent event =
                scheduleCalculator.eventOn(
                        item.getPurchaseDate(),
                        policy,
                        today
                );

        if (event == null || event.routines().isEmpty()) {
            return;
        }

        List<CareRoutineType> routineTypes =
                event.routines()
                        .stream()
                        .map(
                                MaterialCarePolicy
                                        .RoutinePolicy::type
                        )
                        .toList();

        String title = buildTitle(
                policy.materialLabel(),
                routineTypes
        );

        String message = item.getName()
                + "의 권장 관리 시기가 되었어요.";

        String dedupKey =
                "CARE_REMINDER:"
                        + item.getId()
                        + ":"
                        + today;

        notificationRepository.insertCareReminderIfAbsent(
                item.getUser().getId(),
                title,
                message,
                item.getId(),
                item.getName(),
                today,
                routineTypes,
                dedupKey,
                now
        );
    }

    private String buildTitle(
            String materialLabel,
            List<CareRoutineType> routineTypes
    ) {
        if (routineTypes.size() == 1) {
            return materialLabel
                    + " "
                    + routineLabel(routineTypes.getFirst())
                    + " 시기예요";
        }

        if (routineTypes.contains(CareRoutineType.CLEANING)
                && routineTypes.contains(
                CareRoutineType.CONDITIONING
        )) {
            return materialLabel
                    + " 클리닝과 컨디셔닝 시기예요";
        }

        return materialLabel + " 관리 시기예요";
    }

    private String routineLabel(CareRoutineType type) {
        return switch (type) {
            case CLEANING -> "클리닝";
            case CONDITIONING -> "컨디셔닝";
        };
    }
}
