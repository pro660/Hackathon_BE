package org.likelionhsu.hackathon.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "입력값을 확인해 주세요."
    ),

    REQUEST_BODY_INVALID(
            HttpStatus.BAD_REQUEST,
            "REQUEST_BODY_INVALID",
            "요청 본문 형식을 확인해 주세요."
    ),

    PRODUCT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PRODUCT_NOT_FOUND",
            "제품을 찾을 수 없습니다."
    ),

    ENDPOINT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "ENDPOINT_NOT_FOUND",
            "요청한 API 경로를 찾을 수 없습니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(
            HttpStatus status,
            String code,
            String message
    ) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}

