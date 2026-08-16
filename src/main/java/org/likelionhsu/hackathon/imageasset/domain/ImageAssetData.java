package org.likelionhsu.hackathon.imageasset.domain;

import java.time.Instant;

public record ImageAssetData(
        Long id,
        Long ownerUserId,
        ImageAssetPurpose purpose,
        Long userItemId,
        Long aiJobId,
        String publicId,
        String secureUrl,
        String format,
        long bytes,
        int width,
        int height,
        ImageAssetStatus status,
        int sortOrder,
        Instant createdAt,
        Instant activatedAt,
        Instant deletedAt
) {
}
