package org.likelionhsu.hackathon.useritem.dto.response;

import java.time.LocalDate;

public record UserItemPassportPurchaseInfoResponse(
        String purchaseOrderNumber,
        LocalDate purchaseDate,
        Long purchasePrice,
        String purchasePlace
) {
}
