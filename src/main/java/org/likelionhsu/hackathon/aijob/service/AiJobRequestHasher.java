package org.likelionhsu.hackathon.aijob.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class AiJobRequestHasher {

    public String hashPurchaseUtility(
            String normalizedProductId
    ) {
        String canonicalRequest =
                """
                {"type":"PURCHASE_UTILITY","context":{"productId":"%s"}}
                """
                        .formatted(normalizedProductId)
                        .strip();

        return hashCanonical(canonicalRequest);
    }

    public String hashItemAnalysis(
            String normalizedImageAssetId
    ) {
        String canonicalRequest =
                """
                {"type":"ITEM_ANALYSIS","context":{"imageAssetId":"%s"}}
                """
                        .formatted(normalizedImageAssetId)
                        .strip();

        return hashCanonical(canonicalRequest);
    }

    private String hashCanonical(
            String canonicalRequest
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            canonicalRequest.getBytes(
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
