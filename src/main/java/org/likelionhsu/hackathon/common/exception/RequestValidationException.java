package org.likelionhsu.hackathon.common.exception;

import java.util.Objects;

public class RequestValidationException extends RuntimeException {

    private final String field;
    private final String reason;

    public RequestValidationException(
            String field,
            String reason
    ) {
        super(reason);

        this.field = Objects.requireNonNull(
                field,
                "field는 null일 수 없습니다."
        );

        this.reason = Objects.requireNonNull(
                reason,
                "reason은 null일 수 없습니다."
        );
    }

    public String getField() {
        return field;
    }

    public String getReason() {
        return reason;
    }
}