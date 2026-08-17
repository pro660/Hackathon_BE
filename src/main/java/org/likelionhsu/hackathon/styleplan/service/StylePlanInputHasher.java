package org.likelionhsu.hackathon.styleplan.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.springframework.stereotype.Component;

@Component
public class StylePlanInputHasher {

    public String hash(
            StylePlanJobRequest request,
            List<UserItem> items
    ) {
        StringBuilder canonical =
                new StringBuilder();

        canonical.append(request.occasion())
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
                .append(request.language());

        items.stream()
                .sorted((left, right) ->
                        left.getId().compareTo(
                                right.getId()
                        )
                )
                .forEach(item -> canonical
                        .append("|item:")
                        .append(item.getId())
                        .append(':')
                        .append(item.getVersion())
                        .append(':')
                        .append(item.getCategory())
                        .append(':')
                        .append(item.getPrimaryColor())
                        .append(':')
                        .append(item.getMaterial())
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
