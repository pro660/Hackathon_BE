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

    EMAIL_VERIFICATION_INVALID(
            HttpStatus.BAD_REQUEST,
            "EMAIL_VERIFICATION_INVALID",
            "이메일 인증번호가 올바르지 않습니다."
    ),

    EMAIL_VERIFICATION_EXPIRED(
            HttpStatus.BAD_REQUEST,
            "EMAIL_VERIFICATION_EXPIRED",
            "이메일 인증번호가 만료되었습니다."
    ),

    SIGNUP_TOKEN_INVALID(
            HttpStatus.BAD_REQUEST,
            "SIGNUP_TOKEN_INVALID",
            "회원가입 인증 토큰이 유효하지 않습니다."
    ),

    PASSWORD_CONFIRM_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "PASSWORD_CONFIRM_MISMATCH",
            "비밀번호 확인이 일치하지 않습니다."
    ),

    REQUIRED_TERMS_NOT_AGREED(
            HttpStatus.BAD_REQUEST,
            "REQUIRED_TERMS_NOT_AGREED",
            "필수 약관에 동의해야 합니다."
    ),

    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "EMAIL_ALREADY_EXISTS",
            "이미 가입된 이메일입니다."
    ),

    LOGIN_ID_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "LOGIN_ID_ALREADY_EXISTS",
            "이미 사용 중인 로그인 아이디입니다."
    ),

    EMAIL_VERIFICATION_RATE_LIMITED(
            HttpStatus.TOO_MANY_REQUESTS,
            "EMAIL_VERIFICATION_RATE_LIMITED",
            "이메일 인증 요청 횟수를 초과했습니다."
    ),

    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS",
            "로그인 아이디 또는 비밀번호가 올바르지 않습니다."
    ),

    ACCESS_TOKEN_INVALID(
            HttpStatus.UNAUTHORIZED,
            "ACCESS_TOKEN_INVALID",
            "Access Token이 유효하지 않습니다."
    ),

    ACCESS_TOKEN_EXPIRED(
            HttpStatus.UNAUTHORIZED,
            "ACCESS_TOKEN_EXPIRED",
            "Access Token이 만료되었습니다."
    ),

    REFRESH_TOKEN_INVALID(
            HttpStatus.UNAUTHORIZED,
            "REFRESH_TOKEN_INVALID",
            "Refresh Token이 유효하지 않습니다."
    ),

    ACCOUNT_NOT_ACTIVE(
            HttpStatus.FORBIDDEN,
            "ACCOUNT_NOT_ACTIVE",
            "활성 상태의 계정이 아닙니다."
    ),

    ORIGIN_NOT_ALLOWED(
            HttpStatus.FORBIDDEN,
            "ORIGIN_NOT_ALLOWED",
            "허용되지 않은 요청 Origin입니다."
    ),

    RESOURCE_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "RESOURCE_ACCESS_DENIED",
            "해당 작업을 수행할 권한이 없습니다."
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
