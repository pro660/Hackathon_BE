package org.likelionhsu.hackathon.imageasset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetPurpose;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.dto.response.ImageAssetUploadResponse;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.imageasset.storage.ImageStorageException;
import org.likelionhsu.hackathon.imageasset.storage.ImageStoragePort;
import org.likelionhsu.hackathon.imageasset.storage.StoredImage;
import org.likelionhsu.hackathon.imageasset.validation.ImageFileValidator;
import org.likelionhsu.hackathon.imageasset.validation.ValidatedImageFile;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageAssetServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long IMAGE_ASSET_ID = 51L;
    private static final String PUBLIC_ID =
            "wear-it/user-items/asset-51";

    @Mock
    private ImageFileValidator imageFileValidator;

    @Mock
    private ImageStoragePort imageStoragePort;

    @Mock
    private ImageAssetJdbcRepository imageAssetRepository;

    @Mock
    private MultipartFile multipartFile;

    private ImageAssetService service;

    @BeforeEach
    void setUp() {
        service = new ImageAssetService(
                imageFileValidator,
                imageStoragePort,
                imageAssetRepository
        );
    }

    @Test
    void uploadCreatesTemporaryAsset() {
        byte[] bytes = {1, 2, 3};

        when(imageFileValidator.validate(multipartFile))
                .thenReturn(
                        new ValidatedImageFile(
                                bytes,
                                "jpg",
                                640,
                                480
                        )
                );

        StoredImage storedImage = new StoredImage(
                PUBLIC_ID,
                "https://example.com/asset-51.jpg",
                "jpg",
                1234L,
                640,
                480
        );

        when(imageStoragePort.upload(any()))
                .thenReturn(storedImage);

        when(imageAssetRepository.createTemporaryItem(
                USER_ID,
                PUBLIC_ID,
                storedImage.secureUrl(),
                "jpg",
                1234L,
                640,
                480
        )).thenReturn(IMAGE_ASSET_ID);

        ImageAssetUploadResponse response =
                service.uploadTemporaryItemImage(
                        USER_ID,
                        multipartFile
                );

        assertThat(response.imageAssetId())
                .isEqualTo("51");
        assertThat(response.imageUrl())
                .isEqualTo(storedImage.secureUrl());

        verify(imageStoragePort).upload(any());
        verify(imageAssetRepository)
                .createTemporaryItem(
                        USER_ID,
                        PUBLIC_ID,
                        storedImage.secureUrl(),
                        "jpg",
                        1234L,
                        640,
                        480
                );
    }

    @Test
    void invalidFileMapsToImageFileInvalid() {
        when(imageFileValidator.validate(multipartFile))
                .thenThrow(
                        new ImageFileValidator
                                .ValidationException(
                                ImageFileValidator
                                        .Failure.INVALID_FILE,
                                "invalid"
                        )
                );

        assertBusinessError(
                () -> service.uploadTemporaryItemImage(
                        USER_ID,
                        multipartFile
                ),
                ErrorCode.IMAGE_FILE_INVALID
        );

        verify(imageStoragePort, never()).upload(any());
    }

    @Test
    void oversizedFileMapsTo413Error() {
        when(imageFileValidator.validate(multipartFile))
                .thenThrow(
                        new ImageFileValidator
                                .ValidationException(
                                ImageFileValidator
                                        .Failure.FILE_TOO_LARGE,
                                "large"
                        )
                );

        assertBusinessError(
                () -> service.uploadTemporaryItemImage(
                        USER_ID,
                        multipartFile
                ),
                ErrorCode.IMAGE_FILE_TOO_LARGE
        );
    }

    @Test
    void unsupportedFormatMapsTo415Error() {
        when(imageFileValidator.validate(multipartFile))
                .thenThrow(
                        new ImageFileValidator
                                .ValidationException(
                                ImageFileValidator
                                        .Failure.UNSUPPORTED_FORMAT,
                                "unsupported"
                        )
                );

        assertBusinessError(
                () -> service.uploadTemporaryItemImage(
                        USER_ID,
                        multipartFile
                ),
                ErrorCode.IMAGE_FORMAT_UNSUPPORTED
        );
    }

    @Test
    void storageUploadFailureMapsTo502() {
        when(imageFileValidator.validate(multipartFile))
                .thenReturn(
                        new ValidatedImageFile(
                                new byte[] {1},
                                "jpg",
                                1,
                                1
                        )
                );

        when(imageStoragePort.upload(any()))
                .thenThrow(
                        new ImageStorageException(
                                "storage failed"
                        )
                );

        assertBusinessError(
                () -> service.uploadTemporaryItemImage(
                        USER_ID,
                        multipartFile
                ),
                ErrorCode.IMAGE_STORAGE_ERROR
        );

        verifyNoInteractions(imageAssetRepository);
    }

    @Test
    void dbInsertFailureTriggersStorageCompensation() {
        when(imageFileValidator.validate(multipartFile))
                .thenReturn(
                        new ValidatedImageFile(
                                new byte[] {1},
                                "jpg",
                                1,
                                1
                        )
                );

        StoredImage storedImage = new StoredImage(
                PUBLIC_ID,
                "https://example.com/asset-51.jpg",
                "jpg",
                100L,
                1,
                1
        );

        when(imageStoragePort.upload(any()))
                .thenReturn(storedImage);

        when(imageAssetRepository.createTemporaryItem(
                eq(USER_ID),
                eq(PUBLIC_ID),
                eq(storedImage.secureUrl()),
                eq("jpg"),
                eq(100L),
                eq(1),
                eq(1)
        )).thenThrow(
                new IllegalStateException("db failed")
        );

        assertThatThrownBy(
                () -> service.uploadTemporaryItemImage(
                        USER_ID,
                        multipartFile
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                );

        verify(imageStoragePort).delete(PUBLIC_ID);
    }

    @Test
    void temporaryDeleteTransitionsAndDeletesStorage() {
        ImageAssetData temporary =
                asset(ImageAssetStatus.TEMPORARY);

        when(imageAssetRepository.findOwnedItemAsset(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(Optional.of(temporary));

        when(imageAssetRepository.markDeletePending(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(true);

        when(imageAssetRepository.markDeleted(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(true);

        service.deleteTemporaryItemImage(
                USER_ID,
                IMAGE_ASSET_ID
        );

        verify(imageAssetRepository)
                .markDeletePending(
                        USER_ID,
                        IMAGE_ASSET_ID
                );
        verify(imageStoragePort).delete(PUBLIC_ID);
        verify(imageAssetRepository)
                .markDeleted(
                        USER_ID,
                        IMAGE_ASSET_ID
                );
    }

    @Test
    void storageDeleteFailureKeepsDeletePending() {
        ImageAssetData pending =
                asset(ImageAssetStatus.DELETE_PENDING);

        when(imageAssetRepository.findOwnedItemAsset(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(Optional.of(pending));

        doThrow(
                new ImageStorageException("storage failed")
        ).when(imageStoragePort).delete(PUBLIC_ID);

        service.deleteTemporaryItemImage(
                USER_ID,
                IMAGE_ASSET_ID
        );

        verify(imageStoragePort).delete(PUBLIC_ID);
        verify(imageAssetRepository, never())
                .markDeleted(
                        USER_ID,
                        IMAGE_ASSET_ID
                );
    }

    @Test
    void deletedAssetDeleteIsIdempotent() {
        when(imageAssetRepository.findOwnedItemAsset(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(
                Optional.of(
                        asset(ImageAssetStatus.DELETED)
                )
        );

        service.deleteTemporaryItemImage(
                USER_ID,
                IMAGE_ASSET_ID
        );

        verify(imageStoragePort, never()).delete(any());
    }

    @Test
    void activeAssetCannotUseTemporaryDeleteApi() {
        when(imageAssetRepository.findOwnedItemAsset(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(
                Optional.of(
                        asset(ImageAssetStatus.ACTIVE)
                )
        );

        assertBusinessError(
                () -> service.deleteTemporaryItemImage(
                        USER_ID,
                        IMAGE_ASSET_ID
                ),
                ErrorCode.IMAGE_ASSET_STATE_CONFLICT
        );
    }

    @Test
    void unknownOrOtherUsersAssetIsHiddenAs404() {
        when(imageAssetRepository.findOwnedItemAsset(
                USER_ID,
                IMAGE_ASSET_ID
        )).thenReturn(Optional.empty());

        assertBusinessError(
                () -> service.deleteTemporaryItemImage(
                        USER_ID,
                        IMAGE_ASSET_ID
                ),
                ErrorCode.IMAGE_ASSET_NOT_FOUND
        );
    }

    private ImageAssetData asset(
            ImageAssetStatus status
    ) {
        return new ImageAssetData(
                IMAGE_ASSET_ID,
                USER_ID,
                ImageAssetPurpose.ITEM,
                null,
                null,
                PUBLIC_ID,
                "https://example.com/asset-51.jpg",
                "jpg",
                1234L,
                640,
                480,
                status,
                0,
                Instant.parse(
                        "2026-08-17T00:00:00Z"
                ),
                status == ImageAssetStatus.ACTIVE
                        ? Instant.parse(
                        "2026-08-17T00:01:00Z"
                )
                        : null,
                status == ImageAssetStatus.DELETED
                        ? Instant.parse(
                        "2026-08-17T00:02:00Z"
                )
                        : null
        );
    }

    private void assertBusinessError(
            ThrowingAction action,
            ErrorCode expected
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(expected)
                );
    }

    @FunctionalInterface
    private interface ThrowingAction {

        void run();
    }
}
