package org.likelionhsu.hackathon.purchaseutility.ai;

public interface PurchaseUtilityExplanationPort {

    PurchaseUtilityExplanationResult generate(
            PurchaseUtilityExplanationRequest request
    );
}
