package org.likelionhsu.hackathon.aijob.dto.response;

import java.time.Instant;

import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;

public record AiJobCreateResponse(
        String jobId,
        AiJobType type,
        AiJobStatus status,
        Instant createdAt
) {

    public static AiJobCreateResponse from(
            AiJobData job
    ) {
        return new AiJobCreateResponse(
                String.valueOf(job.id()),
                job.type(),
                job.status(),
                job.createdAt()
        );
    }
}
