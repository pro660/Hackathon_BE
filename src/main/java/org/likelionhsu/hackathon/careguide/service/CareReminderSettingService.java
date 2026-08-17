package org.likelionhsu.hackathon.careguide.service;

import java.time.Clock;
import java.time.Instant;

import org.likelionhsu.hackathon.careguide.domain.CareReminderSetting;
import org.likelionhsu.hackathon.careguide.dto.CareReminderSettingRequest;
import org.likelionhsu.hackathon.careguide.dto.CareReminderSettingResponse;
import org.likelionhsu.hackathon.careguide.repository.CareReminderSettingRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CareReminderSettingService {

    private final UserItemRepository userItemRepository;
    private final CareReminderSettingRepository settingRepository;
    private final CareGuideService careGuideService;
    private final Clock clock;

    public CareReminderSettingService(
            UserItemRepository userItemRepository,
            CareReminderSettingRepository settingRepository,
            CareGuideService careGuideService,
            Clock clock
    ) {
        this.userItemRepository = userItemRepository;
        this.settingRepository = settingRepository;
        this.careGuideService = careGuideService;
        this.clock = clock;
    }

    public CareReminderSettingResponse getSetting(
            Long userId,
            Long myItemId
    ) {
        findOwnedActiveItem(userId, myItemId);

        CareReminderSetting setting =
                settingRepository
                        .findByUserItemId(myItemId)
                        .orElse(
                                new CareReminderSetting(
                                        myItemId,
                                        false,
                                        null
                                )
                        );

        return toResponse(setting);
    }

    @Transactional
    public CareReminderSettingResponse updateSetting(
            Long userId,
            Long myItemId,
            CareReminderSettingRequest request
    ) {
        UserItem item = findOwnedActiveItem(
                userId,
                myItemId
        );

        boolean enabled = request.enabled();

        if (enabled
                && !careGuideService
                .resolve(item)
                .reminderAvailable()) {
            throw new RequestValidationException(
                    "enabled",
                    "소재와 구매일 및 지원되는 관리 주기가 있어야 관리 알림을 설정할 수 있습니다."
            );
        }

        CareReminderSetting current =
                settingRepository
                        .findByUserItemId(myItemId)
                        .orElse(null);

        Instant now = clock.instant();
        Instant enabledAt = resolveEnabledAt(
                current,
                enabled,
                now
        );

        settingRepository.upsert(
                myItemId,
                enabled,
                enabledAt,
                now
        );

        return new CareReminderSettingResponse(
                String.valueOf(myItemId),
                enabled,
                enabledAt
        );
    }

    private Instant resolveEnabledAt(
            CareReminderSetting current,
            boolean enabled,
            Instant now
    ) {
        if (!enabled) {
            return null;
        }

        if (current != null
                && current.enabled()
                && current.enabledAt() != null) {
            return current.enabledAt();
        }

        return now;
    }

    private CareReminderSettingResponse toResponse(
            CareReminderSetting setting
    ) {
        return new CareReminderSettingResponse(
                String.valueOf(setting.userItemId()),
                setting.enabled(),
                setting.enabledAt()
        );
    }

    private UserItem findOwnedActiveItem(
            Long userId,
            Long myItemId
    ) {
        return userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        myItemId,
                        userId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.MY_ITEM_NOT_FOUND
                        )
                );
    }
}
