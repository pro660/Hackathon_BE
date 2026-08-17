package org.likelionhsu.hackathon.useritem.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemImageLinkResponse;
import org.likelionhsu.hackathon.useritem.repository.UserItemImageRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserItemImageMutationService {

    private final UserItemImageRepository userItemImageRepository;
    private final ImageAssetJdbcRepository imageAssetRepository;

    public UserItemImageMutationService(
            UserItemImageRepository userItemImageRepository,
            ImageAssetJdbcRepository imageAssetRepository
    ) {
        this.userItemImageRepository =
                Objects.requireNonNull(userItemImageRepository);
        this.imageAssetRepository =
                Objects.requireNonNull(imageAssetRepository);
    }

    @Transactional
    public AttachMutation attach(
            Long userId,
            Long userItemId,
            Long imageAssetId
    ) {
        try {
            return attachLocked(
                    userId,
                    userItemId,
                    imageAssetId
            );
        } catch (DataAccessException exception) {
            if (isLockConflict(exception)) {
                throw stateConflict();
            }

            throw exception;
        }
    }

    @Transactional
    public DeleteMutation deleteLinkedImage(
            Long userId,
            Long userItemId,
            Long imageAssetId
    ) {
        try {
            return deleteLocked(
                    userId,
                    userItemId,
                    imageAssetId
            );
        } catch (DataAccessException exception) {
            if (isLockConflict(exception)) {
                throw stateConflict();
            }

            throw exception;
        }
    }

    private AttachMutation attachLocked(
            Long userId,
            Long userItemId,
            Long imageAssetId
    ) {
        lockOwnedItem(userId, userItemId);

        ImageAssetData asset = lockOwnedAsset(
                userId,
                imageAssetId
        );

        if (asset.status() == ImageAssetStatus.ACTIVE) {
            if (Objects.equals(
                    asset.userItemId(),
                    userItemId
            )) {
                return new AttachMutation(
                        toResponse(asset),
                        List.of()
                );
            }

            throw stateConflict();
        }

        if (asset.status()
                != ImageAssetStatus.TEMPORARY
                || asset.userItemId() != null) {
            throw stateConflict();
        }

        List<ImageAssetData> previousActiveImages =
                imageAssetRepository
                        .findActiveOwnedItemAssetsForUpdate(
                                userId,
                                userItemId
                        );

        imageAssetRepository
                .markActiveImagesDeletePending(
                        userId,
                        userItemId
                );

        boolean activated =
                imageAssetRepository
                        .activateTemporaryForItem(
                                userId,
                                imageAssetId,
                                userItemId
                        );

        if (!activated) {
            throw stateConflict();
        }

        return new AttachMutation(
                new UserItemImageLinkResponse(
                        String.valueOf(asset.id()),
                        asset.secureUrl()
                ),
                List.copyOf(previousActiveImages)
        );
    }

    private DeleteMutation deleteLocked(
            Long userId,
            Long userItemId,
            Long imageAssetId
    ) {
        lockOwnedItem(userId, userItemId);

        ImageAssetData asset = lockOwnedAsset(
                userId,
                imageAssetId
        );

        if (!Objects.equals(
                asset.userItemId(),
                userItemId
        )) {
            throw stateConflict();
        }

        return switch (asset.status()) {
            case ACTIVE -> {
                boolean pending =
                        imageAssetRepository
                                .markLinkedActiveDeletePending(
                                        userId,
                                        userItemId,
                                        imageAssetId
                                );

                if (!pending) {
                    throw stateConflict();
                }

                yield new DeleteMutation(asset);
            }
            case DELETE_PENDING ->
                    new DeleteMutation(asset);
            case DELETED ->
                    new DeleteMutation(null);
            case TEMPORARY ->
                    throw stateConflict();
        };
    }

    private void lockOwnedItem(
            Long userId,
            Long userItemId
    ) {
        boolean found =
                userItemImageRepository
                        .lockOwnedActiveItem(
                                userId,
                                userItemId
                        );

        if (!found) {
            throw new BusinessException(
                    ErrorCode.MY_ITEM_NOT_FOUND
            );
        }
    }

    private ImageAssetData lockOwnedAsset(
            Long userId,
            Long imageAssetId
    ) {
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
    }

    private UserItemImageLinkResponse toResponse(
            ImageAssetData asset
    ) {
        return new UserItemImageLinkResponse(
                String.valueOf(asset.id()),
                asset.secureUrl()
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

    public record AttachMutation(
            UserItemImageLinkResponse response,
            List<ImageAssetData> cleanupTargets
    ) {

        public AttachMutation {
            cleanupTargets =
                    cleanupTargets == null
                            ? List.of()
                            : List.copyOf(cleanupTargets);
        }
    }

    public record DeleteMutation(
            ImageAssetData cleanupTarget
    ) {
    }
}
