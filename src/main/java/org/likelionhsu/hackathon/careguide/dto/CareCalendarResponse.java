package org.likelionhsu.hackathon.careguide.dto;

import java.time.LocalDate;
import java.util.List;

import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.careguide.domain.CareUnavailableReason;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;

public record CareCalendarResponse(
        String myItemId,
        MaterialGroup material,
        LocalDate purchaseDate,
        String month,
        boolean available,
        CareUnavailableReason unavailableReason,
        List<Event> events
) {

    public record Event(
            LocalDate date,
            List<Routine> routines
    ) {
    }

    public record Routine(
            CareRoutineType type,
            String title
    ) {
    }
}
