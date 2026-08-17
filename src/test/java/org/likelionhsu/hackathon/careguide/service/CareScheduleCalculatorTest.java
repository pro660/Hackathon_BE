package org.likelionhsu.hackathon.careguide.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.careguide.domain.CareIntervalUnit;
import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicy;

class CareScheduleCalculatorTest {

    private final CareScheduleCalculator calculator =
            new CareScheduleCalculator();

    @Test
    void leatherCalendarUsesPurchaseDateAsStableAnchor() {
        List<CareScheduleCalculator.ScheduleEvent> events =
                calculator.eventsForMonth(
                        LocalDate.of(2026, 8, 10),
                        leatherPolicy(),
                        YearMonth.of(2027, 2)
                );

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().date())
                .isEqualTo(LocalDate.of(2027, 2, 10));
        assertThat(events.getFirst().routines())
                .extracting(MaterialCarePolicy.RoutinePolicy::type)
                .containsExactly(
                        CareRoutineType.CLEANING,
                        CareRoutineType.CONDITIONING
                );
    }

    @Test
    void monthEndAnchorDoesNotDriftAcrossOccurrences() {
        MaterialCarePolicy policy = new MaterialCarePolicy(
                "가죽",
                "title",
                "description",
                List.of(
                        new MaterialCarePolicy.RoutinePolicy(
                                CareRoutineType.CLEANING,
                                3,
                                CareIntervalUnit.MONTH,
                                "클리닝",
                                "설명"
                        )
                ),
                storage()
        );

        List<CareScheduleCalculator.ScheduleEvent> events =
                calculator.eventsForMonth(
                        LocalDate.of(2026, 1, 31),
                        policy,
                        YearMonth.of(2026, 7)
                );

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().date())
                .isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void nextRecommendedReturnsNearestDueRoutine() {
        CareScheduleCalculator.NextCare next =
                calculator.nextRecommended(
                        LocalDate.of(2026, 8, 10),
                        leatherPolicy(),
                        LocalDate.of(2026, 8, 18)
                );

        assertThat(next.date())
                .isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(next.routines())
                .extracting(MaterialCarePolicy.RoutinePolicy::type)
                .containsExactly(CareRoutineType.CLEANING);
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
                storage()
        );
    }

    private MaterialCarePolicy.StoragePolicy storage() {
        return new MaterialCarePolicy.StoragePolicy(
                List.of("피해야 할 환경"),
                List.of("습기 관리"),
                List.of("보관법")
        );
    }
}
