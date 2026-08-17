package org.likelionhsu.hackathon.aijob.service;

import java.util.Objects;
import java.util.function.Supplier;

import org.likelionhsu.hackathon.aijob.repository.AiJobCreationPolicyRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiJobCreationPolicyService {

    private final AiJobCreationPolicyRepository repository;
    private final int dailyLimit;

    public AiJobCreationPolicyService(
            AiJobCreationPolicyRepository repository,
            @Value("${OPENAI_DAILY_LIMIT_PER_USER:10}")
            int dailyLimit
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository는 null일 수 없습니다."
        );

        if (dailyLimit < 1) {
            throw new IllegalArgumentException(
                    "OPENAI_DAILY_LIMIT_PER_USER는 1 이상이어야 합니다."
            );
        }

        this.dailyLimit = dailyLimit;
    }

    @Transactional
    public <T> T execute(
            Long userId,
            String idempotencyKey,
            Supplier<T> creation
    ) {
        Objects.requireNonNull(
                userId,
                "userId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                creation,
                "creation은 null일 수 없습니다."
        );

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "idempotencyKey는 비어 있을 수 없습니다."
            );
        }

        repository.lockUser(userId);

        repository.expireStaleRunningJobs(userId);

        if (repository
                .existsRunningJobExceptIdempotencyKey(
                        userId,
                        idempotencyKey
                )) {
            throw new BusinessException(
                    ErrorCode.AI_JOB_ALREADY_RUNNING
            );
        }

        int recentCount = repository
                .countCreatedInLastTwentyFourHoursExceptIdempotencyKey(
                        userId,
                        idempotencyKey
                );

        if (recentCount >= dailyLimit) {
            throw new BusinessException(
                    ErrorCode.AI_DAILY_LIMIT_EXCEEDED
            );
        }

        return creation.get();
    }
}
