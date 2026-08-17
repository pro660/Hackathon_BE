package org.likelionhsu.hackathon.careguide.policy;

import java.util.List;

import org.likelionhsu.hackathon.careguide.domain.CareIntervalUnit;
import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;

public record MaterialCarePolicy(
        String materialLabel,
        String summaryTitle,
        String summaryDescription,
        List<RoutinePolicy> routines,
        StoragePolicy storageGuide
) {

    public record RoutinePolicy(
            CareRoutineType type,
            int intervalValue,
            CareIntervalUnit intervalUnit,
            String title,
            String description
    ) {
    }

    public record StoragePolicy(
            List<String> avoidEnvironments,
            List<String> humidityManagement,
            List<String> recommendedStorage
    ) {
    }
}
