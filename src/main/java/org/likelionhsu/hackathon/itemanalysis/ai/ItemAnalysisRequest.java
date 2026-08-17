package org.likelionhsu.hackathon.itemanalysis.ai;

import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;

public record ItemAnalysisRequest(
        String imageAssetId,
        String imageUrl,
        String format,
        int width,
        int height
) {

    public static ItemAnalysisRequest from(
            ImageAssetData asset
    ) {
        return new ItemAnalysisRequest(
                String.valueOf(asset.id()),
                asset.secureUrl(),
                asset.format(),
                asset.width(),
                asset.height()
        );
    }
}
