package org.likelionhsu.hackathon.imageasset.config;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "app.image-assets.cleanup"
)
public record ImageAssetCleanupProperties(
        boolean enabled,
        Duration temporaryTtl,
        Duration fixedDelay,
        Duration initialDelay,
        int batchSize
) {

    public ImageAssetCleanupProperties {
        temporaryTtl = requirePositive(
                temporaryTtl,
                "temporaryTtl"
        );
        fixedDelay = requirePositive(
                fixedDelay,
                "fixedDelay"
        );
        initialDelay = requireNonNegative(
                initialDelay,
                "initialDelay"
        );

        if (batchSize < 1) {
            throw new IllegalArgumentException(
                    "batchSize는 1 이상이어야 합니다."
            );
        }
    }

    private static Duration requirePositive(
            Duration value,
            String name
    ) {
        Objects.requireNonNull(
                value,
                name + "은 null일 수 없습니다."
        );

        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    name + "은 0보다 커야 합니다."
            );
        }

        return value;
    }

    private static Duration requireNonNegative(
            Duration value,
            String name
    ) {
        Objects.requireNonNull(
                value,
                name + "은 null일 수 없습니다."
        );

        if (value.isNegative()) {
            throw new IllegalArgumentException(
                    name + "은 0 이상이어야 합니다."
            );
        }

        return value;
    }
}
