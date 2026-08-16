package org.likelionhsu.hackathon.aijob.dto.response;

import java.time.Instant;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;

public record AiJobResponse(
        String jobId,
        AiJobType type,
        AiJobStatus status,
        JsonNode result,
        JsonNode fallback,
        AiJobErrorResponse error,
        Instant createdAt,
        Instant completedAt
) {

    public static AiJobResponse from(
            AiJobData job,
            ObjectMapper objectMapper
    ) {
        return new AiJobResponse(
                String.valueOf(job.id()),
                job.type(),
                job.status(),
                parseJson(
                        job.resultJson(),
                        objectMapper
                ),
                parseJson(
                        job.fallbackJson(),
                        objectMapper
                ),
                AiJobErrorResponse.fromCode(
                        job.errorCode()
                ),
                job.createdAt(),
                job.completedAt()
        );
    }

    private static JsonNode parseJson(
            String json,
            ObjectMapper objectMapper
    ) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "저장된 AI Job JSON을 읽을 수 없습니다.",
                    exception
            );
        }
    }
}
