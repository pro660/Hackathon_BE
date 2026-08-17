package org.likelionhsu.hackathon.careguide.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.likelionhsu.hackathon.careguide.domain.CareIntervalUnit;
import org.likelionhsu.hackathon.careguide.domain.CareUnavailableReason;
import org.likelionhsu.hackathon.careguide.dto.CareCalendarResponse;
import org.likelionhsu.hackathon.careguide.dto.CareGuideResponse;
import org.likelionhsu.hackathon.careguide.dto.StorageGuideResponse;
import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicy;
import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicyRegistry;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CareGuideService {

    private static final ZoneId SEOUL =
            ZoneId.of("Asia/Seoul");

    private static final String RECOMMENDATION_NOTICE =
            "서비스 권장 관리 주기이며 제조사 공식 주기와 다를 수 있습니다.";

    private final UserItemRepository userItemRepository;
    private final MaterialCarePolicyRegistry policyRegistry;
    private final CareScheduleCalculator scheduleCalculator;
    private final Clock clock;

    public CareGuideService(
            UserItemRepository userItemRepository,
            MaterialCarePolicyRegistry policyRegistry,
            CareScheduleCalculator scheduleCalculator,
            Clock clock
    ) {
        this.userItemRepository = userItemRepository;
        this.policyRegistry = policyRegistry;
        this.scheduleCalculator = scheduleCalculator;
        this.clock = clock;
    }

    public CareGuideResponse getCareGuide(
            Long userId,
            Long myItemId
    ) {
        UserItem item = findOwnedActiveItem(
                userId,
                myItemId
        );

        ResolvedPolicy resolved = resolve(item);

        if (resolved.policy() == null) {
            return new CareGuideResponse(
                    String.valueOf(item.getId()),
                    null,
                    item.getMaterialSource(),
                    toAvailability(resolved),
                    null,
                    List.of(),
                    null,
                    RECOMMENDATION_NOTICE
            );
        }

        List<CareGuideResponse.Routine> routines =
                resolved.policy()
                        .routines()
                        .stream()
                        .map(this::toRoutineResponse)
                        .toList();

        CareGuideResponse.NextRecommendedCare next =
                resolved.calendarAvailable()
                        ? toNextRecommendedCare(
                                scheduleCalculator.nextRecommended(
                                        item.getPurchaseDate(),
                                        resolved.policy(),
                                        LocalDate.now(
                                                clock.withZone(SEOUL)
                                        )
                                )
                        )
                        : null;

        return new CareGuideResponse(
                String.valueOf(item.getId()),
                item.getMaterial(),
                item.getMaterialSource(),
                toAvailability(resolved),
                new CareGuideResponse.Summary(
                        resolved.policy().materialLabel(),
                        resolved.policy().summaryTitle(),
                        resolved.policy().summaryDescription()
                ),
                routines,
                next,
                RECOMMENDATION_NOTICE
        );
    }

    public CareCalendarResponse getCareCalendar(
            Long userId,
            Long myItemId,
            String monthValue
    ) {
        UserItem item = findOwnedActiveItem(
                userId,
                myItemId
        );

        YearMonth month = parseMonth(monthValue);
        ResolvedPolicy resolved = resolve(item);

        if (!resolved.calendarAvailable()) {
            return new CareCalendarResponse(
                    String.valueOf(item.getId()),
                    item.getMaterial(),
                    item.getPurchaseDate(),
                    month.toString(),
                    false,
                    resolved.unavailableReason(),
                    List.of()
            );
        }

        List<CareCalendarResponse.Event> events =
                scheduleCalculator
                        .eventsForMonth(
                                item.getPurchaseDate(),
                                resolved.policy(),
                                month
                        )
                        .stream()
                        .map(event ->
                                new CareCalendarResponse.Event(
                                        event.date(),
                                        event.routines()
                                                .stream()
                                                .map(routine ->
                                                        new CareCalendarResponse.Routine(
                                                                routine.type(),
                                                                routine.title()
                                                        )
                                                )
                                                .toList()
                                )
                        )
                        .toList();

        return new CareCalendarResponse(
                String.valueOf(item.getId()),
                item.getMaterial(),
                item.getPurchaseDate(),
                month.toString(),
                true,
                null,
                events
        );
    }

    public StorageGuideResponse getStorageGuide(
            Long userId,
            Long myItemId
    ) {
        UserItem item = findOwnedActiveItem(
                userId,
                myItemId
        );

        ResolvedPolicy resolved = resolve(item);

        if (resolved.policy() == null) {
            return new StorageGuideResponse(
                    String.valueOf(item.getId()),
                    null,
                    false,
                    CareUnavailableReason.MATERIAL_REQUIRED,
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        MaterialCarePolicy.StoragePolicy storage =
                resolved.policy().storageGuide();

        return new StorageGuideResponse(
                String.valueOf(item.getId()),
                item.getMaterial(),
                true,
                null,
                resolved.policy().materialLabel(),
                storage.avoidEnvironments(),
                storage.humidityManagement(),
                storage.recommendedStorage()
        );
    }

    public ResolvedPolicy resolve(UserItem item) {
        if (item.getMaterial() == null) {
            return new ResolvedPolicy(
                    null,
                    false,
                    false,
                    false,
                    false,
                    CareUnavailableReason.MATERIAL_REQUIRED
            );
        }

        MaterialCarePolicy policy =
                policyRegistry.get(item.getMaterial());

        if (policy.routines().isEmpty()) {
            return new ResolvedPolicy(
                    policy,
                    true,
                    true,
                    false,
                    false,
                    CareUnavailableReason.ROUTINE_UNAVAILABLE
            );
        }

        if (item.getPurchaseDate() == null) {
            return new ResolvedPolicy(
                    policy,
                    true,
                    true,
                    false,
                    false,
                    CareUnavailableReason.PURCHASE_DATE_REQUIRED
            );
        }

        return new ResolvedPolicy(
                policy,
                true,
                true,
                true,
                true,
                null
        );
    }

    private CareGuideResponse.Availability toAvailability(
            ResolvedPolicy resolved
    ) {
        return new CareGuideResponse.Availability(
                resolved.careGuideAvailable(),
                resolved.storageGuideAvailable(),
                resolved.calendarAvailable(),
                resolved.reminderAvailable(),
                resolved.unavailableReason()
        );
    }

    private CareGuideResponse.Routine toRoutineResponse(
            MaterialCarePolicy.RoutinePolicy routine
    ) {
        return new CareGuideResponse.Routine(
                routine.type(),
                routine.title(),
                routine.description(),
                routine.intervalValue(),
                routine.intervalUnit(),
                formatInterval(
                        routine.intervalValue(),
                        routine.intervalUnit()
                )
        );
    }

    private CareGuideResponse.NextRecommendedCare
    toNextRecommendedCare(
            CareScheduleCalculator.NextCare next
    ) {
        if (next == null) {
            return null;
        }

        return new CareGuideResponse.NextRecommendedCare(
                next.date(),
                next.routines()
                        .stream()
                        .map(MaterialCarePolicy.RoutinePolicy::type)
                        .toList()
        );
    }

    private String formatInterval(
            int value,
            CareIntervalUnit unit
    ) {
        String suffix = switch (unit) {
            case DAY -> "일";
            case WEEK -> "주";
            case MONTH -> "개월";
            case YEAR -> "년";
        };

        return value + suffix;
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) {
            throw new RequestValidationException(
                    "month",
                    "YYYY-MM 형식의 조회 월을 입력해 주세요."
            );
        }

        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new RequestValidationException(
                    "month",
                    "YYYY-MM 형식이어야 합니다."
            );
        }
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

    public record ResolvedPolicy(
            MaterialCarePolicy policy,
            boolean careGuideAvailable,
            boolean storageGuideAvailable,
            boolean calendarAvailable,
            boolean reminderAvailable,
            CareUnavailableReason unavailableReason
    ) {
    }
}
