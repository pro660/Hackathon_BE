package org.likelionhsu.hackathon.place.client;

public class PlaceSearchException extends RuntimeException {

    private final FailureKind failureKind;

    public PlaceSearchException(
            FailureKind failureKind,
            String message
    ) {
        super(message);
        this.failureKind = failureKind;
    }

    public PlaceSearchException(
            FailureKind failureKind,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.failureKind = failureKind;
    }

    public FailureKind failureKind() {
        return failureKind;
    }

    public enum FailureKind {
        UNAVAILABLE,
        TIMEOUT,
        INVALID_RESPONSE
    }
}
