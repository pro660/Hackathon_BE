package org.likelionhsu.hackathon.careguide.dto;

import java.time.LocalDate;
import java.util.List;

import org.likelionhsu.hackathon.careguide.domain.CareIntervalUnit;
import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.careguide.domain.CareUnavailableReason;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;

public record CareGuideResponse(
        String myItemId,
        MaterialGroup material,
        MaterialSource materialSource,
        Availability availability,
        Summary summary,
        List<Routine> routines,
        NextRecommendedCare nextRecommendedCare,
        String recommendationNotice
) {

    public record Availability(
            boolean careGuideAvailable,
            boolean storageGuideAvailable,
            boolean calendarAvailable,
            boolean reminderAvailable,
            CareUnavailableReason unavailableReason
    ) {
    }

    public record Summary(
            String materialLabel,
            String title,
            String description
    ) {
    }

    public record Routine(
            CareRoutineType type,
            String title,
            String description,
            int intervalValue,
            CareIntervalUnit intervalUnit,
            String intervalLabel
    ) {
    }

    public record NextRecommendedCare(
            LocalDate date,
            List<CareRoutineType> routineTypes
    ) {
    }
}
