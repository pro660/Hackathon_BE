package org.likelionhsu.hackathon.itemanalysis.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.likelionhsu.hackathon.imageasset.domain.ImageAssetData;
import org.springframework.stereotype.Component;

@Component
public class ItemAnalysisInputHasher {

    public String hash(
            ImageAssetData asset
    ) {
        String canonicalInput =
                String.join(
                        "|",
                        "ITEM_ANALYSIS",
                        String.valueOf(asset.id()),
                        asset.publicId(),
                        asset.format(),
                        String.valueOf(asset.bytes()),
                        String.valueOf(asset.width()),
                        String.valueOf(asset.height())
                );

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            canonicalInput.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 해시 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }
}
