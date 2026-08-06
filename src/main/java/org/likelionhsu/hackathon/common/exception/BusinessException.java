package org.likelionhsu.hackathon.common.exception;

import java.util.Objects;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(
                Objects.requireNonNull(
                        errorCode,
                        "errorCode는 null일 수 없습니다."
                ).message()
        );

        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

