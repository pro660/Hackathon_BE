package org.likelionhsu.hackathon.aijob.service;

import java.sql.SQLException;
import java.util.Objects;

import org.likelionhsu.hackathon.aijob.domain.AiJobData;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.repository.AiJobJdbcRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemAnalysisAiJobCreationService {

    private final AiJobJdbcRepository aiJobRepository;
    private final ImageAssetJdbcRepository imageAssetRepository;

    public ItemAnalysisAiJobCreationService(
            AiJobJdbcRepository aiJobRepository,
            ImageAssetJdbcRepository imageAssetRepository
    ) {
        this.aiJobRepository =
                Objects.requireNonNull(aiJobRepository);
        this.imageAssetRepository =
                Objects.requireNonNull(imageAssetRepository);
    }

    @Transactional
    public AiJobData createPendingAndBind(
            Long userId,
            Long imageAssetId,
            String idempotencyKey,
            String requestHash,
            String model,
            String promptVersion
    ) {
        long jobId = aiJobRepository.createPending(
                userId,
                AiJobType.ITEM_ANALYSIS,
                idempotencyKey,
                requestHash,
                model,
                promptVersion
        );

        ImageAssetData asset = lockOwnedAsset(
                userId,
                imageAssetId
        );

        validateAvailableForAnalysis(
                userId,
                asset
        );

        boolean bound =
                imageAssetRepository
                        .bindAiJobToTemporaryItemAsset(
                                userId,
                                imageAssetId,
                                jobId
                        );

        if (!bound) {
            throw stateConflict();
        }

        return aiJobRepository
                .findOwned(
                        userId,
                        jobId
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "생성한 ITEM_ANALYSIS AI Job을 조회할 수 없습니다."
                        )
                );
    }

    private ImageAssetData lockOwnedAsset(
            Long userId,
            Long imageAssetId
    ) {
        try {
            return imageAssetRepository
                    .lockOwnedItemAsset(
                            userId,
                            imageAssetId
                    )
                    .orElseThrow(() ->
                            new BusinessException(
                                    ErrorCode
                                            .IMAGE_ASSET_NOT_FOUND
                            )
                    );
        } catch (DataAccessException exception) {
            if (isLockConflict(exception)) {
                throw stateConflict();
            }

            throw exception;
        }
    }

    private void validateAvailableForAnalysis(
            Long userId,
            ImageAssetData asset
    ) {
        if (asset.status()
                != ImageAssetStatus.TEMPORARY
                || asset.userItemId() != null
                || asset.deletedAt() != null) {
            throw stateConflict();
        }

        if (asset.aiJobId() == null) {
            return;
        }

        AiJobData linkedJob =
                aiJobRepository
                        .findOwned(
                                userId,
                                asset.aiJobId()
                        )
                        .orElseThrow(
                                this::stateConflict
                        );

        if (linkedJob.type()
                != AiJobType.ITEM_ANALYSIS) {
            throw stateConflict();
        }

        if (linkedJob.status() != AiJobStatus.PENDING
                && linkedJob.status()
                != AiJobStatus.PROCESSING) {
            return;
        }

        boolean timedOut =
                aiJobRepository.markTimedOutIfStale(
                        userId,
                        linkedJob.id()
                );

        if (timedOut) {
            return;
        }

        AiJobData refreshed =
                aiJobRepository
                        .findOwned(
                                userId,
                                linkedJob.id()
                        )
                        .orElseThrow(
                                this::stateConflict
                        );

        if (refreshed.status() != AiJobStatus.PENDING
                && refreshed.status()
                != AiJobStatus.PROCESSING) {
            return;
        }

        throw new BusinessException(
                ErrorCode.IMAGE_ASSET_IN_USE
        );
    }

    private BusinessException stateConflict() {
        return new BusinessException(
                ErrorCode.IMAGE_ASSET_STATE_CONFLICT
        );
    }

    private boolean isLockConflict(
            DataAccessException exception
    ) {
        if (exception
                instanceof PessimisticLockingFailureException) {
            return true;
        }

        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof SQLException sqlException
                    && isMySqlNowaitConflict(sqlException)) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private boolean isMySqlNowaitConflict(
            SQLException exception
    ) {
        return exception.getErrorCode() == 3572
                && "HY000".equals(
                exception.getSQLState()
        );
    }
}
