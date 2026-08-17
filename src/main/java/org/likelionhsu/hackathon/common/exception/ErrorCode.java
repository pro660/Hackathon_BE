package org.likelionhsu.hackathon.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "입력값을 확인해 주세요."
    ),

    PREFERENCE_UPDATE_CONFLICT(
            HttpStatus.CONFLICT,
            "PREFERENCE_UPDATE_CONFLICT",
            "취향 정보를 수정하는 중 충돌이 발생했습니다. 다시 시도해 주세요."
    ),

    USER_PROFILE_UPDATE_CONFLICT(
            HttpStatus.CONFLICT,
            "USER_PROFILE_UPDATE_CONFLICT",
            "사용자 정보를 수정하는 중 충돌이 발생했습니다. 다시 시도해 주세요."
    ),

    PREFERENCE_REQUIRED(
            HttpStatus.CONFLICT,
            "PREFERENCE_REQUIRED",
            "제품 추천을 받으려면 먼저 취향 정보를 등록해 주세요."
    ),

    RECOMMENDATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "RECOMMENDATION_NOT_FOUND",
            "추천 결과를 찾을 수 없습니다."
    ),

    PURCHASE_UTILITY_ANALYSIS_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PURCHASE_UTILITY_ANALYSIS_NOT_FOUND",
            "구매 활용성 분석 결과를 찾을 수 없습니다."
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

    PROFILE_INCOMPLETE(
            HttpStatus.BAD_REQUEST,
            "PROFILE_INCOMPLETE",
            "필수 프로필 정보를 확인해 주세요."
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

    SOCIAL_ACCOUNT_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "SOCIAL_ACCOUNT_ALREADY_EXISTS",
            "이미 연결된 소셜 계정입니다."
    ),

    SOCIAL_EMAIL_CONFLICT(
            HttpStatus.CONFLICT,
            "SOCIAL_EMAIL_CONFLICT",
            "이미 가입된 이메일과 소셜 계정이 충돌합니다."
    ),

    EMAIL_VERIFICATION_RATE_LIMITED(
            HttpStatus.TOO_MANY_REQUESTS,
            "EMAIL_VERIFICATION_RATE_LIMITED",
            "이메일 인증 요청 횟수를 초과했습니다."
    ),

    EMAIL_PROVIDER_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "EMAIL_PROVIDER_UNAVAILABLE",
            "이메일 인증 서비스를 일시적으로 사용할 수 없습니다."
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

    REAUTHENTICATION_FAILED(
            HttpStatus.UNAUTHORIZED,
            "REAUTHENTICATION_FAILED",
            "계정 재인증에 실패했습니다."
    ),

    REAUTHENTICATION_METHOD_NOT_AVAILABLE(
            HttpStatus.CONFLICT,
            "REAUTHENTICATION_METHOD_NOT_AVAILABLE",
            "현재 계정에서 사용할 수 없는 재인증 방식입니다."
    ),

    REAUTH_TOKEN_INVALID(
            HttpStatus.UNAUTHORIZED,
            "REAUTH_TOKEN_INVALID",
            "계정 재인증 토큰이 유효하지 않습니다."
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

    OAUTH_STATE_INVALID(
            HttpStatus.BAD_REQUEST,
            "OAUTH_STATE_INVALID",
            "OAuth state가 유효하지 않습니다."
    ),

    OAUTH_PROVIDER_ERROR(
            HttpStatus.BAD_GATEWAY,
            "OAUTH_PROVIDER_ERROR",
            "소셜 로그인 공급자 연동에 실패했습니다."
    ),

    RESOURCE_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "RESOURCE_ACCESS_DENIED",
            "해당 작업을 수행할 권한이 없습니다."
    ),

    RESOURCE_VERSION_CONFLICT(
            HttpStatus.CONFLICT,
            "RESOURCE_VERSION_CONFLICT",
            "리소스를 수정하는 중 충돌이 발생했습니다. 다시 시도해 주세요."
    ),

    IDEMPOTENCY_KEY_CONFLICT(
            HttpStatus.CONFLICT,
            "IDEMPOTENCY_KEY_CONFLICT",
            "동일한 Idempotency-Key에 다른 요청이 사용되었습니다."
    ),

    AI_JOB_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AI_JOB_NOT_FOUND",
            "AI 작업을 찾을 수 없습니다."
    ),

    AI_JOB_ALREADY_RUNNING(
            HttpStatus.CONFLICT,
            "AI_JOB_ALREADY_RUNNING",
            "이미 처리 중인 AI 작업이 있습니다."
    ),

    AI_DAILY_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "AI_DAILY_LIMIT_EXCEEDED",
            "최근 24시간 AI 작업 요청 한도를 초과했습니다."
    ),

    IMAGE_FILE_INVALID(
            HttpStatus.BAD_REQUEST,
            "IMAGE_FILE_INVALID",
            "올바른 이미지 파일을 업로드해 주세요."
    ),

    IMAGE_ASSET_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "IMAGE_ASSET_NOT_FOUND",
            "이미지를 찾을 수 없습니다."
    ),

    IMAGE_ASSET_STATE_CONFLICT(
            HttpStatus.CONFLICT,
            "IMAGE_ASSET_STATE_CONFLICT",
            "현재 이미지 상태에서는 요청한 작업을 수행할 수 없습니다."
    ),

    IMAGE_ASSET_ANALYSIS_MISMATCH(
            HttpStatus.CONFLICT,
            "IMAGE_ASSET_ANALYSIS_MISMATCH",
            "AI 분석에 사용된 이미지와 연결하려는 이미지가 일치하지 않습니다."
    ),

    IMAGE_ASSET_IN_USE(
            HttpStatus.CONFLICT,
            "IMAGE_ASSET_IN_USE",
            "현재 사용 중인 이미지에는 요청한 작업을 수행할 수 없습니다."
    ),

    IMAGE_FILE_TOO_LARGE(
            HttpStatus.CONTENT_TOO_LARGE,
            "IMAGE_FILE_TOO_LARGE",
            "이미지 파일은 10MB 이하여야 합니다."
    ),

    IMAGE_FORMAT_UNSUPPORTED(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "IMAGE_FORMAT_UNSUPPORTED",
            "JPEG 또는 PNG 이미지만 업로드할 수 있습니다."
    ),

    IMAGE_STORAGE_ERROR(
            HttpStatus.BAD_GATEWAY,
            "IMAGE_STORAGE_ERROR",
            "이미지 저장소 연동에 실패했습니다."
    ),

    MY_ITEM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "MY_ITEM_NOT_FOUND",
            "마이 아이템을 찾을 수 없습니다."
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
