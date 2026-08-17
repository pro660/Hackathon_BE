package org.likelionhsu.hackathon.careguide.dto;

import java.util.List;

import org.likelionhsu.hackathon.careguide.domain.CareUnavailableReason;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;

public record StorageGuideResponse(
        String myItemId,
        MaterialGroup material,
        boolean available,
        CareUnavailableReason unavailableReason,
        String materialLabel,
        List<String> avoidEnvironments,
        List<String> humidityManagement,
        List<String> recommendedStorage
) {
}
