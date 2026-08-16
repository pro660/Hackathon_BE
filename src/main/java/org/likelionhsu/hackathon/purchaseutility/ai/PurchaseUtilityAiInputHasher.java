package org.likelionhsu.hackathon.purchaseutility.ai;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class PurchaseUtilityAiInputHasher {

    private static final String HASH_SCHEMA =
            "purchase-utility-ai-input-v1";

    public String hash(
            PurchaseUtilityExplanationRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request는 null일 수 없습니다."
        );

        StringBuilder canonical = new StringBuilder();

        append(canonical, HASH_SCHEMA);
        append(canonical, request.scorePolicyVersion());
        append(canonical, request.product().productId());
        append(canonical, request.product().name());
        append(canonical, request.product().category());
        append(canonical, request.product().primaryColor());
        append(canonical, request.utilityScore());
        append(
                canonical,
                request.factors().preferenceTagFitScore()
        );
        append(
                canonical,
                request.factors().styleCombinationScore()
        );
        append(
                canonical,
                request.factors().seasonUsabilityScore()
        );
        append(
                canonical,
                request.factors().ownedCategoryCombinationScore()
        );
        append(canonical, request.compatibleItemCount());
        append(canonical, request.compatibleItems().size());

        for (PurchaseUtilityExplanationRequest.CompatibleItemContext item
                : request.compatibleItems()) {
            append(canonical, item.myItemId());
            append(canonical, item.name());
            append(canonical, item.category());
            append(canonical, item.primaryColor());
            append(canonical, item.reason());
        }

        append(canonical, request.careDifficulty());
        append(canonical, request.language());

        return sha256(canonical.toString());
    }

    private void append(
            StringBuilder target,
            Object value
    ) {
        String normalized = normalize(value);

        if (normalized == null) {
            target.append("-1:");
        } else {
            target
                    .append(normalized.length())
                    .append(':')
                    .append(normalized);
        }

        target.append('|');
    }

    private String normalize(
            Object value
    ) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal
                    .stripTrailingZeros()
                    .toPlainString();
        }

        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        return String.valueOf(value);
    }

    private String sha256(
            String value
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
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
