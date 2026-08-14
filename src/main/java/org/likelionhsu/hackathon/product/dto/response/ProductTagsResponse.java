package org.likelionhsu.hackathon.product.dto.response;

import java.util.List;

public record ProductTagsResponse(
        List<String> styles,
        List<String> seasons,
        List<String> occasions,
        List<String> features
) {
}