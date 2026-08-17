package org.likelionhsu.hackathon.useritem.dto.request;

import java.time.LocalDate;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UserItemCreateRequest(
        Long productId,

        @Size(max = 100, message = "100자 이하여야 합니다.")
        String brandName,

        @NotBlank(message = "필수 입력값입니다.")
        @Size(max = 200, message = "200자 이하여야 합니다.")
        String name,

        @NotNull(message = "필수 입력값입니다.")
        ItemCategory category,

        ColorGroup primaryColor,
        MaterialGroup material,
        MaterialSource materialSource,

        @PastOrPresent(message = "미래 날짜일 수 없습니다.")
        LocalDate purchaseDate,

        @PositiveOrZero(message = "0 이상이어야 합니다.")
        Long purchasePrice,

        @Size(max = 100, message = "100자 이하여야 합니다.")
        String purchaseOrderNumber,

        @Size(max = 200, message = "200자 이하여야 합니다.")
        String purchasePlace,

        @Size(max = 1000, message = "1000자 이하여야 합니다.")
        String memo,

        Long aiJobId,
        LocalDate nextCareDate
) {
    public UserItemCreateRequest(
            Long productId,
            String brandName,
            String name,
            ItemCategory category,
            ColorGroup primaryColor,
            MaterialGroup material,
            MaterialSource materialSource,
            LocalDate purchaseDate,
            Long purchasePrice,
            String memo,
            Long aiJobId,
            LocalDate nextCareDate
    ) {
        this(
                productId,
                brandName,
                name,
                category,
                primaryColor,
                material,
                materialSource,
                purchaseDate,
                purchasePrice,
                null,
                null,
                memo,
                aiJobId,
                nextCareDate
        );
    }
}
