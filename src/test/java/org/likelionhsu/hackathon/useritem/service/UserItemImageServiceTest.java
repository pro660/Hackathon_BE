package org.likelionhsu.hackathon.useritem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetPurpose;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.imageasset.storage.ImageStorageException;
import org.likelionhsu.hackathon.imageasset.storage.ImageStoragePort;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemImageLinkResponse;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserItemImageServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ITEM_ID = 10L;
    private static final Long IMAGE_ID = 51L;

    @Mock
    private UserItemImageMutationService mutationService;

    @Mock
    private ImageStoragePort imageStoragePort;

    @Mock
    private ImageAssetJdbcRepository imageAssetRepository;

    private UserItemImageService service;

    @BeforeEach
    void setUp() {
        service = new UserItemImageService(
                mutationService,
                imageStoragePort,
                imageAssetRepository
        );
    }

    @Test
    void replacementCleansPreviousImagesAfterMutation() {
        ImageAssetData previous =
                asset(
                        41L,
                        ITEM_ID,
                        ImageAssetStatus.ACTIVE
                );

        UserItemImageLinkResponse response =
                new UserItemImageLinkResponse(
                        "51",
                        "https://example.com/51.jpg"
                );

        when(mutationService.attach(
                USER_ID,
                ITEM_ID,
                IMAGE_ID
        )).thenReturn(
                new UserItemImageMutationService
                        .AttachMutation(
                        response,
                        List.of(previous)
                )
        );

        when(imageAssetRepository.markDeleted(
                USER_ID,
                41L
        )).thenReturn(true);

        UserItemImageLinkResponse result =
                service.attach(
                        USER_ID,
                        ITEM_ID,
                        IMAGE_ID
                );

        assertThat(result).isEqualTo(response);
        verify(imageStoragePort).delete(
                previous.publicId()
        );
        verify(imageAssetRepository).markDeleted(
                USER_ID,
                41L
        );
    }

    @Test
    void storageFailureLeavesReplacementDeletePending() {
        ImageAssetData previous =
                asset(
                        41L,
                        ITEM_ID,
                        ImageAssetStatus.ACTIVE
                );

        when(mutationService.attach(
                USER_ID,
                ITEM_ID,
                IMAGE_ID
        )).thenReturn(
                new UserItemImageMutationService
                        .AttachMutation(
                        new UserItemImageLinkResponse(
                                "51",
                                "https://example.com/51.jpg"
                        ),
                        List.of(previous)
                )
        );

        doThrow(
                new ImageStorageException(
                        "storage failed"
                )
        ).when(imageStoragePort)
                .delete(previous.publicId());

        service.attach(
                USER_ID,
                ITEM_ID,
                IMAGE_ID
        );

        verify(imageAssetRepository, never())
                .markDeleted(USER_ID, 41L);
    }

    @Test
    void linkedDeleteCleansPendingImage() {
        ImageAssetData active =
                asset(
                        IMAGE_ID,
                        ITEM_ID,
                        ImageAssetStatus.ACTIVE
                );

        when(mutationService.deleteLinkedImage(
                USER_ID,
                ITEM_ID,
                IMAGE_ID
        )).thenReturn(
                new UserItemImageMutationService
                        .DeleteMutation(active)
        );

        service.delete(
                USER_ID,
                ITEM_ID,
                IMAGE_ID
        );

        verify(imageStoragePort).delete(
                active.publicId()
        );
        verify(imageAssetRepository).markDeleted(
                USER_ID,
                IMAGE_ID
        );
    }

    @Test
    void alreadyDeletedImageNeedsNoStorageCall() {
        when(mutationService.deleteLinkedImage(
                USER_ID,
                ITEM_ID,
                IMAGE_ID
        )).thenReturn(
                new UserItemImageMutationService
                        .DeleteMutation(null)
        );

        service.delete(
                USER_ID,
                ITEM_ID,
                IMAGE_ID
        );

        verify(imageStoragePort, never())
                .delete(org.mockito.ArgumentMatchers.any());
    }

    private ImageAssetData asset(
            Long imageId,
            Long userItemId,
            ImageAssetStatus status
    ) {
        return new ImageAssetData(
                imageId,
                USER_ID,
                ImageAssetPurpose.ITEM,
                userItemId,
                null,
                "wear-it/user-items/" + imageId,
                "https://example.com/"
                        + imageId
                        + ".jpg",
                "jpg",
                1024L,
                640,
                480,
                status,
                0,
                Instant.parse(
                        "2026-08-17T00:00:00Z"
                ),
                Instant.parse(
                        "2026-08-17T00:01:00Z"
                ),
                null
        );
    }
}
