package org.likelionhsu.hackathon.careguide.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicy;
import org.springframework.stereotype.Component;

@Component
public class CareScheduleCalculator {

    public List<ScheduleEvent> eventsForMonth(
            LocalDate purchaseDate,
            MaterialCarePolicy policy,
            YearMonth month
    ) {
        Map<LocalDate, List<MaterialCarePolicy.RoutinePolicy>>
                routinesByDate = new TreeMap<>();

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        for (MaterialCarePolicy.RoutinePolicy routine
                : policy.routines()) {
            long occurrence = 1L;

            while (true) {
                LocalDate date = occurrenceDate(
                        purchaseDate,
                        routine,
                        occurrence
                );

                if (date.isAfter(monthEnd)) {
                    break;
                }

                if (!date.isBefore(monthStart)) {
                    routinesByDate
                            .computeIfAbsent(
                                    date,
                                    ignored -> new ArrayList<>()
                            )
                            .add(routine);
                }

                occurrence++;
            }
        }

        return routinesByDate.entrySet()
                .stream()
                .map(entry -> new ScheduleEvent(
                        entry.getKey(),
                        List.copyOf(entry.getValue())
                ))
                .toList();
    }

    public ScheduleEvent eventOn(
            LocalDate purchaseDate,
            MaterialCarePolicy policy,
            LocalDate date
    ) {
        return eventsForMonth(
                purchaseDate,
                policy,
                YearMonth.from(date)
        )
                .stream()
                .filter(event -> event.date().equals(date))
                .findFirst()
                .orElse(null);
    }

    public NextCare nextRecommended(
            LocalDate purchaseDate,
            MaterialCarePolicy policy,
            LocalDate today
    ) {
        TreeMap<
                LocalDate,
                List<MaterialCarePolicy.RoutinePolicy>
                > nextByDate = new TreeMap<>();

        for (MaterialCarePolicy.RoutinePolicy routine
                : policy.routines()) {
            long occurrence = 1L;
            LocalDate date;

            do {
                date = occurrenceDate(
                        purchaseDate,
                        routine,
                        occurrence
                );
                occurrence++;
            } while (date.isBefore(today));

            nextByDate
                    .computeIfAbsent(
                            date,
                            ignored -> new ArrayList<>()
                    )
                    .add(routine);
        }

        if (nextByDate.isEmpty()) {
            return null;
        }

        Map.Entry<
                LocalDate,
                List<MaterialCarePolicy.RoutinePolicy>
                > first = nextByDate.firstEntry();

        return new NextCare(
                first.getKey(),
                List.copyOf(first.getValue())
        );
    }

    private LocalDate occurrenceDate(
            LocalDate anchor,
            MaterialCarePolicy.RoutinePolicy routine,
            long occurrence
    ) {
        long amount = Math.multiplyExact(
                (long) routine.intervalValue(),
                occurrence
        );

        return switch (routine.intervalUnit()) {
            case DAY -> anchor.plusDays(amount);
            case WEEK -> anchor.plusWeeks(amount);
            case MONTH -> anchor.plusMonths(amount);
            case YEAR -> anchor.plusYears(amount);
        };
    }

    public record ScheduleEvent(
            LocalDate date,
            List<MaterialCarePolicy.RoutinePolicy> routines
    ) {
    }

    public record NextCare(
            LocalDate date,
            List<MaterialCarePolicy.RoutinePolicy> routines
    ) {
    }
}
