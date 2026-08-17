package org.likelionhsu.hackathon.imageasset.service;

import java.util.Objects;
import java.util.UUID;

import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.dto.response.ImageAssetUploadResponse;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.imageasset.storage.ImageStorageException;
import org.likelionhsu.hackathon.imageasset.storage.ImageStoragePort;
import org.likelionhsu.hackathon.imageasset.storage.ImageStorageUploadRequest;
import org.likelionhsu.hackathon.imageasset.storage.StoredImage;
import org.likelionhsu.hackathon.imageasset.validation.ImageFileValidator;
import org.likelionhsu.hackathon.imageasset.validation.ValidatedImageFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageAssetService {

    private static final Logger log =
            LoggerFactory.getLogger(ImageAssetService.class);

    private final ImageFileValidator imageFileValidator;
    private final ImageStoragePort imageStoragePort;
    private final ImageAssetJdbcRepository imageAssetRepository;

    public ImageAssetService(
            ImageFileValidator imageFileValidator,
            ImageStoragePort imageStoragePort,
            ImageAssetJdbcRepository imageAssetRepository
    ) {
        this.imageFileValidator =
                Objects.requireNonNull(imageFileValidator);
        this.imageStoragePort =
                Objects.requireNonNull(imageStoragePort);
        this.imageAssetRepository =
                Objects.requireNonNull(imageAssetRepository);
    }

    public ImageAssetUploadResponse uploadTemporaryItemImage(
            Long ownerUserId,
            MultipartFile file
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );

        ValidatedImageFile validated =
                validate(file);

        String generatedPublicId =
                UUID.randomUUID().toString();

        StoredImage storedImage;

        try {
            storedImage = imageStoragePort.upload(
                    new ImageStorageUploadRequest(
                            validated.bytes(),
                            generatedPublicId
                    )
            );
        } catch (ImageStorageException exception) {
            throw new BusinessException(
                    ErrorCode.IMAGE_STORAGE_ERROR
            );
        }

        try {
            long imageAssetId =
                    imageAssetRepository.createTemporaryItem(
                            ownerUserId,
                            storedImage.publicId(),
                            storedImage.secureUrl(),
                            storedImage.format(),
                            storedImage.bytes(),
                            storedImage.width(),
                            storedImage.height()
                    );

            return new ImageAssetUploadResponse(
                    String.valueOf(imageAssetId),
                    storedImage.secureUrl()
            );
        } catch (RuntimeException exception) {
            compensateUploadedImage(storedImage.publicId());
            throw exception;
        }
    }

    public void deleteTemporaryItemImage(
            Long ownerUserId,
            Long imageAssetId
    ) {
        Objects.requireNonNull(
                ownerUserId,
                "ownerUserId는 null일 수 없습니다."
        );
        Objects.requireNonNull(
                imageAssetId,
                "imageAssetId는 null일 수 없습니다."
        );

        ImageAssetData asset = findOwned(
                ownerUserId,
                imageAssetId
        );

        if (asset.status() == ImageAssetStatus.TEMPORARY
                && imageAssetRepository
                        .isUsedByRunningAiJob(
                                ownerUserId,
                                imageAssetId
                        )) {
            throw new BusinessException(
                    ErrorCode.IMAGE_ASSET_IN_USE
            );
        }

        switch (asset.status()) {
            case TEMPORARY ->
                    deleteTemporary(
                            ownerUserId,
                            imageAssetId,
                            asset
                    );
            case DELETE_PENDING ->
                    tryFinalizeDelete(asset);
            case DELETED -> {
                // Idempotent no-op.
            }
            case ACTIVE -> throw new BusinessException(
                    ErrorCode.IMAGE_ASSET_STATE_CONFLICT
            );
        }
    }

    private void deleteTemporary(
            Long ownerUserId,
            Long imageAssetId,
            ImageAssetData originalAsset
    ) {
        boolean transitioned =
                imageAssetRepository.markDeletePending(
                        ownerUserId,
                        imageAssetId
                );

        if (transitioned) {
            tryFinalizeDelete(originalAsset);
            return;
        }

        ImageAssetData current = findOwned(
                ownerUserId,
                imageAssetId
        );

        switch (current.status()) {
            case DELETE_PENDING ->
                    tryFinalizeDelete(current);
            case DELETED -> {
                // Concurrent idempotent delete already completed.
            }
            case TEMPORARY -> {
                if (imageAssetRepository
                        .isUsedByRunningAiJob(
                                ownerUserId,
                                imageAssetId
                        )) {
                    throw new BusinessException(
                            ErrorCode.IMAGE_ASSET_IN_USE
                    );
                }

                throw new BusinessException(
                        ErrorCode.IMAGE_ASSET_STATE_CONFLICT
                );
            }
            case ACTIVE ->
                    throw new BusinessException(
                            ErrorCode.IMAGE_ASSET_STATE_CONFLICT
                    );
        }
    }

    private void tryFinalizeDelete(
            ImageAssetData asset
    ) {
        try {
            imageStoragePort.delete(asset.publicId());
        } catch (ImageStorageException exception) {
            log.warn(
                    "ImageAsset Cloudinary 삭제에 실패했습니다. "
                            + "DELETE_PENDING으로 재시도합니다. "
                            + "imageAssetId={}",
                    asset.id(),
                    exception
            );
            return;
        }

        imageAssetRepository.markDeleted(
                asset.ownerUserId(),
                asset.id()
        );
    }

    private ImageAssetData findOwned(
            Long ownerUserId,
            Long imageAssetId
    ) {
        return imageAssetRepository
                .findOwnedItemAsset(
                        ownerUserId,
                        imageAssetId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.IMAGE_ASSET_NOT_FOUND
                        )
                );
    }

    private ValidatedImageFile validate(
            MultipartFile file
    ) {
        try {
            return imageFileValidator.validate(file);
        } catch (
                ImageFileValidator.ValidationException
                        exception
        ) {
            throw new BusinessException(
                    switch (exception.failure()) {
                        case INVALID_FILE ->
                                ErrorCode.IMAGE_FILE_INVALID;
                        case FILE_TOO_LARGE ->
                                ErrorCode.IMAGE_FILE_TOO_LARGE;
                        case UNSUPPORTED_FORMAT ->
                                ErrorCode.IMAGE_FORMAT_UNSUPPORTED;
                    }
            );
        }
    }

    private void compensateUploadedImage(
            String publicId
    ) {
        try {
            imageStoragePort.delete(publicId);
        } catch (RuntimeException cleanupException) {
            log.warn(
                    "ImageAsset DB 저장 실패 후 Cloudinary "
                            + "보상 삭제에도 실패했습니다. publicId={}",
                    publicId,
                    cleanupException
            );
        }
    }
}
