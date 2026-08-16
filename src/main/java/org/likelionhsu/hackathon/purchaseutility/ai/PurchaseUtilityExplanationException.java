package org.likelionhsu.hackathon.purchaseutility.ai;

import java.util.Objects;

public final class PurchaseUtilityExplanationException
        extends RuntimeException {

    private final FailureKind failureKind;

    public PurchaseUtilityExplanationException(
            FailureKind failureKind,
            String message
    ) {
        super(message);
        this.failureKind = Objects.requireNonNull(
                failureKind,
                "failureKind는 null일 수 없습니다."
        );
    }

    public PurchaseUtilityExplanationException(
            FailureKind failureKind,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.failureKind = Objects.requireNonNull(
                failureKind,
                "failureKind는 null일 수 없습니다."
        );
    }

    public FailureKind getFailureKind() {
        return failureKind;
    }

    public boolean isRetryable() {
        return failureKind == FailureKind.TRANSIENT_PROVIDER
                || failureKind == FailureKind.INVALID_RESPONSE;
    }

    public enum FailureKind {
        TRANSIENT_PROVIDER,
        INVALID_RESPONSE,
        NON_RETRYABLE_PROVIDER,
        CONFIGURATION
    }
}
