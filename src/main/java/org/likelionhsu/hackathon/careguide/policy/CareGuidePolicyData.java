package org.likelionhsu.hackathon.careguide.policy;

import java.util.Map;

import org.likelionhsu.hackathon.common.enums.MaterialGroup;

public record CareGuidePolicyData(
        Map<MaterialGroup, MaterialCarePolicy> materials
) {
}
