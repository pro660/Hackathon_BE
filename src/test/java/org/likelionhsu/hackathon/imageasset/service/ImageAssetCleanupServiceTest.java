package org.likelionhsu.hackathon.imageasset.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.imageasset.config.ImageAssetCleanupProperties;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetPurpose;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.imageasset.storage.ImageStorageException;
import org.likelionhsu.hackathon.imageasset.storage.ImageStoragePort;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageAssetCleanupServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-17T00:00:00Z");
    private static final Instant CUTOFF =
            Instant.parse("2026-08-16T00:00:00Z");

    @Mock
    private ImageAssetJdbcRepository imageAssetRepository;

    @Mock
    private ImageStoragePort imageStoragePort;

    private ImageAssetCleanupService service;

    @BeforeEach
    void setUp() {
        ImageAssetCleanupProperties properties =
                new ImageAssetCleanupProperties(
                        false,
                        Duration.ofHours(24),
                        Duration.ofHours(1),
                        Duration.ofSeconds(30),
                        100
                );

        service = new ImageAssetCleanupService(
                imageAssetRepository,
                imageStoragePort,
                Clock.fixed(NOW, ZoneOffset.UTC),
                properties
        );
    }

    @Test
    void expiredTemporaryAssetIsQueuedForDeletion() {
        ImageAssetData expired =
                asset(
                        51L,
                        ImageAssetStatus.TEMPORARY,
                        CUTOFF.minusSeconds(1)
                );

        when(imageAssetRepository
                .findExpiredTemporaryCandidates(
                        CUTOFF,
                        100
                ))
                .thenReturn(List.of(expired));

        when(imageAssetRepository
                .findDeletePending(100))
                .thenReturn(List.of());

        service.cleanupOnce();

        verify(imageAssetRepository)
                .markExpiredTemporaryDeletePending(
                        1L,
                        51L,
                        CUTOFF
                );
    }

    @Test
    void storageFailureDoesNotStopPendingBatch() {
        ImageAssetData first =
                asset(
                        51L,
                        ImageAssetStatus.DELETE_PENDING,
                        CUTOFF.minusSeconds(10)
                );
        ImageAssetData second =
                asset(
                        52L,
                        ImageAssetStatus.DELETE_PENDING,
                        CUTOFF.minusSeconds(5)
                );

        when(imageAssetRepository
                .findExpiredTemporaryCandidates(
                        CUTOFF,
                        100
                ))
                .thenReturn(List.of());

        when(imageAssetRepository
                .findDeletePending(100))
                .thenReturn(List.of(first, second));

        doThrow(
                new ImageStorageException("delete failed")
        ).when(imageStoragePort)
                .delete(first.publicId());

        service.cleanupOnce();

        verify(imageStoragePort)
                .delete(first.publicId());
        verify(imageStoragePort)
                .delete(second.publicId());
        verify(imageAssetRepository)
                .markDeleted(1L, 52L);
    }

    @Test
    void successfulPendingDeleteIsMarkedDeleted() {
        ImageAssetData pending =
                asset(
                        51L,
                        ImageAssetStatus.DELETE_PENDING,
                        CUTOFF.minusSeconds(10)
                );

        when(imageAssetRepository
                .findExpiredTemporaryCandidates(
                        CUTOFF,
                        100
                ))
                .thenReturn(List.of());

        when(imageAssetRepository
                .findDeletePending(100))
                .thenReturn(List.of(pending));

        service.cleanupOnce();

        verify(imageStoragePort)
                .delete(pending.publicId());
        verify(imageAssetRepository)
                .markDeleted(1L, 51L);
    }

    private ImageAssetData asset(
            Long id,
            ImageAssetStatus status,
            Instant createdAt
    ) {
        return new ImageAssetData(
                id,
                1L,
                ImageAssetPurpose.ITEM,
                null,
                null,
                "wear-it/user-items/" + id,
                "https://example.com/" + id + ".jpg",
                "jpg",
                2048L,
                1200,
                900,
                status,
                0,
                createdAt,
                null,
                null
        );
    }
}
