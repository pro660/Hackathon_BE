package org.likelionhsu.hackathon.imageasset.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.likelionhsu.hackathon.imageasset.config.ImageAssetCleanupProperties;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.repository.ImageAssetJdbcRepository;
import org.likelionhsu.hackathon.imageasset.storage.ImageStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImageAssetCleanupService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ImageAssetCleanupService.class
            );

    private final ImageAssetJdbcRepository imageAssetRepository;
    private final ImageStoragePort imageStoragePort;
    private final Clock clock;
    private final ImageAssetCleanupProperties properties;

    public ImageAssetCleanupService(
            ImageAssetJdbcRepository imageAssetRepository,
            ImageStoragePort imageStoragePort,
            Clock clock,
            ImageAssetCleanupProperties properties
    ) {
        this.imageAssetRepository =
                Objects.requireNonNull(imageAssetRepository);
        this.imageStoragePort =
                Objects.requireNonNull(imageStoragePort);
        this.clock = Objects.requireNonNull(clock);
        this.properties =
                Objects.requireNonNull(properties);
    }

    public void cleanupOnce() {
        expireTemporaryAssets();
        retryDeletePendingAssets();
    }

    private void expireTemporaryAssets() {
        Instant cutoff = Instant.now(clock)
                .minus(properties.temporaryTtl());

        List<ImageAssetData> candidates =
                imageAssetRepository
                        .findExpiredTemporaryCandidates(
                                cutoff,
                                properties.batchSize()
                        );

        for (ImageAssetData candidate : candidates) {
            try {
                imageAssetRepository
                        .markExpiredTemporaryDeletePending(
                                candidate.ownerUserId(),
                                candidate.id(),
                                cutoff
                        );
            } catch (RuntimeException exception) {
                log.warn(
                        "만료 TEMPORARY ImageAsset 전환에 "
                                + "실패했습니다. imageAssetId={}",
                        candidate.id(),
                        exception
                );
            }
        }
    }

    private void retryDeletePendingAssets() {
        List<ImageAssetData> pendingAssets =
                imageAssetRepository.findDeletePending(
                        properties.batchSize()
                );

        for (ImageAssetData asset : pendingAssets) {
            try {
                imageStoragePort.delete(
                        asset.publicId()
                );

                imageAssetRepository.markDeleted(
                        asset.ownerUserId(),
                        asset.id()
                );
            } catch (RuntimeException exception) {
                log.warn(
                        "DELETE_PENDING ImageAsset 저장소 삭제에 "
                                + "실패했습니다. 다음 주기에 재시도합니다. "
                                + "imageAssetId={}",
                        asset.id(),
                        exception
                );
            }
        }
    }
}
