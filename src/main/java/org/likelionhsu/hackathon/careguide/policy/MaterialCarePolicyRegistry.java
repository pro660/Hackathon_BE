package org.likelionhsu.hackathon.careguide.policy;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class MaterialCarePolicyRegistry {

    private static final String RESOURCE =
            "data/care-guide-rules.json";

    private final Map<MaterialGroup, MaterialCarePolicy> policies;

    public MaterialCarePolicyRegistry(JsonMapper jsonMapper) {
        Objects.requireNonNull(
                jsonMapper,
                "jsonMapper는 null일 수 없습니다."
        );

        CareGuidePolicyData data = read(jsonMapper);
        validate(data);
        this.policies = Map.copyOf(data.materials());
    }

    public MaterialCarePolicy get(MaterialGroup material) {
        Objects.requireNonNull(
                material,
                "material은 null일 수 없습니다."
        );

        MaterialCarePolicy policy = policies.get(material);
        if (policy == null) {
            throw new IllegalStateException(
                    "관리 정책을 찾을 수 없습니다: " + material
            );
        }
        return policy;
    }

    private CareGuidePolicyData read(JsonMapper jsonMapper) {
        ClassPathResource resource =
                new ClassPathResource(RESOURCE);

        try (InputStream inputStream = resource.getInputStream()) {
            return jsonMapper.readValue(
                    inputStream,
                    CareGuidePolicyData.class
            );
        } catch (IOException | JacksonException exception) {
            throw new IllegalStateException(
                    "관리 가이드 정책 JSON을 읽을 수 없습니다.",
                    exception
            );
        }
    }

    private void validate(CareGuidePolicyData data) {
        if (data == null || data.materials() == null) {
            throw new IllegalStateException(
                    "관리 가이드 정책의 materials가 필요합니다."
            );
        }

        for (MaterialGroup material : MaterialGroup.values()) {
            MaterialCarePolicy policy =
                    data.materials().get(material);

            if (policy == null) {
                throw new IllegalStateException(
                        "관리 가이드 정책이 누락되었습니다: "
                                + material
                );
            }

            validatePolicy(material, policy);
        }
    }

    private void validatePolicy(
            MaterialGroup material,
            MaterialCarePolicy policy
    ) {
        requireText(
                policy.materialLabel(),
                material + ".materialLabel"
        );
        requireText(
                policy.summaryTitle(),
                material + ".summaryTitle"
        );
        requireText(
                policy.summaryDescription(),
                material + ".summaryDescription"
        );

        if (policy.routines() == null) {
            throw new IllegalStateException(
                    material + ".routines는 null일 수 없습니다."
            );
        }

        Set<CareRoutineType> seenTypes =
                EnumSet.noneOf(CareRoutineType.class);

        for (MaterialCarePolicy.RoutinePolicy routine
                : policy.routines()) {
            if (routine == null || routine.type() == null) {
                throw new IllegalStateException(
                        material
                                + ".routines의 type이 필요합니다."
                );
            }

            if (!seenTypes.add(routine.type())) {
                throw new IllegalStateException(
                        material
                                + "에 중복 관리 종류가 있습니다: "
                                + routine.type()
                );
            }

            if (routine.intervalValue() <= 0
                    || routine.intervalUnit() == null) {
                throw new IllegalStateException(
                        material
                                + "."
                                + routine.type()
                                + "의 관리 주기가 올바르지 않습니다."
                );
            }

            requireText(
                    routine.title(),
                    material + "." + routine.type() + ".title"
            );
            requireText(
                    routine.description(),
                    material + "." + routine.type() + ".description"
            );
        }

        if ((material == MaterialGroup.OTHER
                || material == MaterialGroup.UNKNOWN)
                && !policy.routines().isEmpty()) {
            throw new IllegalStateException(
                    material
                            + "에는 정기 관리 주기를 둘 수 없습니다."
            );
        }

        MaterialCarePolicy.StoragePolicy storage =
                policy.storageGuide();

        if (storage == null) {
            throw new IllegalStateException(
                    material + ".storageGuide가 필요합니다."
            );
        }

        validateTextList(
                storage.avoidEnvironments(),
                material + ".storageGuide.avoidEnvironments"
        );
        validateTextList(
                storage.humidityManagement(),
                material + ".storageGuide.humidityManagement"
        );
        validateTextList(
                storage.recommendedStorage(),
                material + ".storageGuide.recommendedStorage"
        );
    }

    private void validateTextList(
            List<String> values,
            String field
    ) {
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException(
                    field + "는 비어 있을 수 없습니다."
            );
        }

        for (String value : values) {
            requireText(value, field);
        }
    }

    private void requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    field + "는 비어 있을 수 없습니다."
            );
        }
    }
}
