package org.likelionhsu.hackathon.useritem.dto.response;

public record UserItemPassportResponse(
        String myItemId,
        UserItemPassportProductInfoResponse productInfo,
        UserItemPassportPurchaseInfoResponse purchaseInfo
) {
}
