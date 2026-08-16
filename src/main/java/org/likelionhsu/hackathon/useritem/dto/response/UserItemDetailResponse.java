package org.likelionhsu.hackathon.useritem.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;

public record UserItemDetailResponse(
        String myItemId,
        String linkedProductId,
        String brandName,
        String name,
        ItemCategory category,
        ColorGroup primaryColor,
        MaterialGroup material,
        MaterialSource materialSource,
        LocalDate purchaseDate,
        Long purchasePrice,
        String memo,
        LocalDate nextCareDate,
        String aiJobId,
        List<UserItemImageResponse> images,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
