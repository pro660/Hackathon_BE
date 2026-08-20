package org.likelionhsu.hackathon.aijob.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

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

    public String hashStylePlan(
            String occasion,
            List<String> styleTags,
            String weatherCondition,
            boolean prioritizeOwnedItems,
            String language
    ) {
        return hashStylePlan(
                occasion,
                deriveAxisLevel(styleTags, "CASUAL", "FORMAL"),
                deriveAxisLevel(styleTags, "NEAT", "GLAMOROUS"),
                styleTags,
                weatherCondition,
                prioritizeOwnedItems,
                language
        );
    }

    public String hashStylePlan(
            String occasion,
            int casualFormalLevel,
            int neatGlamorousLevel,
            List<String> styleTags,
            String weatherCondition,
            boolean prioritizeOwnedItems,
            String language
    ) {
        Objects.requireNonNull(occasion, "occasion");
        Objects.requireNonNull(styleTags, "styleTags");
        Objects.requireNonNull(language, "language");

        List<String> normalizedStyleTags = styleTags
                .stream()
                .map(String::trim)
                .sorted()
                .toList();

        String styleTagsJson = "[\""
                + String.join("\",\"", normalizedStyleTags)
                + "\"]";
        String weatherJson = weatherCondition == null
                ? "null"
                : "\"" + weatherCondition.trim() + "\"";

        String canonicalRequest =
                "{\"type\":\"STYLE_PLAN\",\"context\":{"
                        + "\"occasion\":\"" + occasion.trim() + "\","
                        + "\"casualFormalLevel\":" + casualFormalLevel + ","
                        + "\"neatGlamorousLevel\":" + neatGlamorousLevel + ","
                        + "\"styleTags\":" + styleTagsJson + ","
                        + "\"weatherCondition\":" + weatherJson + ","
                        + "\"prioritizeOwnedItems\":" + prioritizeOwnedItems + ","
                        + "\"language\":\"" + language.trim() + "\"}}";

        return hashCanonical(canonicalRequest);
    }

    private int deriveAxisLevel(
            List<String> styleTags,
            String lowTag,
            String highTag
    ) {
        if (styleTags == null || styleTags.isEmpty()) {
            return 5;
        }

        boolean low = styleTags.stream()
                .map(String::trim)
                .anyMatch(lowTag::equals);
        boolean high = styleTags.stream()
                .map(String::trim)
                .anyMatch(highTag::equals);

        if (low == high) {
            return 5;
        }

        return high ? 8 : 3;
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
