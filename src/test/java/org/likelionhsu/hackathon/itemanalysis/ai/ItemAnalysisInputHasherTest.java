package org.likelionhsu.hackathon.itemanalysis.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetPurpose;
import org.likelionhsu.hackathon.imageasset.domain.ImageAssetStatus;

class ItemAnalysisInputHasherTest {

    private final ItemAnalysisInputHasher hasher =
            new ItemAnalysisInputHasher();

    @Test
    void imageMetadataHashIsDeterministic() {
        ImageAssetData asset =
                new ImageAssetData(
                        51L,
                        1L,
                        ImageAssetPurpose.ITEM,
                        null,
                        9101L,
                        "wear-it/user-items/test-image",
                        "https://example.com/test-image.jpg",
                        "jpg",
                        2048L,
                        1200,
                        900,
                        ImageAssetStatus.TEMPORARY,
                        0,
                        Instant.parse(
                                "2026-08-17T01:00:00Z"
                        ),
                        null,
                        null
                );

        assertThat(
                hasher.hash(asset)
        ).isEqualTo(
                "127659f4d995c9330ab41bb015892cf8e6f3f58ecfaf04a367965a1888d8e394"
        );

        assertThat(hasher.hash(asset))
                .hasSize(64);
    }
}
