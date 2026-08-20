package org.likelionhsu.hackathon.styleplan.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class StylePlanInputHasher {

    public String hash(
            StylePlanRecommendationContext context
    ) {
        StringBuilder canonical =
                new StringBuilder();

        StylePlanJobRequest request =
                context.request();

        canonical.append(request.occasion())
                .append('|')
                .append(request.casualFormalLevel())
                .append('|')
                .append(request.neatGlamorousLevel())
                .append('|')
                .append(String.join(
                        ",",
                        request.styleTags()
                ))
                .append('|')
                .append(request.weatherCondition())
                .append('|')
                .append(request.prioritizeOwnedItems())
                .append('|')
                .append(request.language())
                .append("|prefStyle:")
                .append(String.join(
                        ",",
                        context.preferredStyleTags()
                ))
                .append("|prefColor:")
                .append(String.join(
                        ",",
                        context.preferredColors()
                ))
                .append("|prefCategory:")
                .append(String.join(
                        ",",
                        context.preferredCategories()
                ));

        context.ownedItems().forEach(item ->
                canonical
                        .append("|item:")
                        .append(item.myItemId())
                        .append(':')
                        .append(item.version())
                        .append(':')
                        .append(item.name())
                        .append(':')
                        .append(item.category())
                        .append(':')
                        .append(item.primaryColor())
                        .append(':')
                        .append(item.material())
                        .append(':')
                        .append(item.imageUrl())
                        .append(':')
                        .append(item.score())
        );

        context.productCandidates().forEach(product ->
                canonical
                        .append("|product:")
                        .append(product.productId())
                        .append(':')
                        .append(product.name())
                        .append(':')
                        .append(product.category())
                        .append(':')
                        .append(product.primaryColor())
                        .append(':')
                        .append(product.material())
                        .append(':')
                        .append(product.imageUrl())
                        .append(':')
                        .append(String.join(
                                ",",
                                product.tags()
                        ))
                        .append(':')
                        .append(product.score())
        );

        return sha256(canonical.toString());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(
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
