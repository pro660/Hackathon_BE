package org.likelionhsu.hackathon.imageasset.scheduler;

import org.likelionhsu.hackathon.imageasset.service.ImageAssetCleanupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.image-assets.cleanup",
        name = "enabled",
        havingValue = "true"
)
public class ImageAssetCleanupScheduler {

    private final ImageAssetCleanupService cleanupService;

    public ImageAssetCleanupScheduler(
            ImageAssetCleanupService cleanupService
    ) {
        this.cleanupService = cleanupService;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.image-assets.cleanup.fixed-delay:1h}",
            initialDelayString =
                    "${app.image-assets.cleanup.initial-delay:30s}"
    )
    public void cleanup() {
        cleanupService.cleanupOnce();
    }
}
