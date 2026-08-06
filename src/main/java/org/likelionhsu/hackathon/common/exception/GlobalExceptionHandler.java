package org.likelionhsu.hackathon.common.exception;

import java.util.List;

import org.likelionhsu.hackathon.common.response.ErrorResponse;
import org.likelionhsu.hackathon.common.response.FieldErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception
    ) {
        ErrorCode errorCode =
                ErrorCode.ENDPOINT_NOT_FOUND;

        ErrorResponse response =
                ErrorResponse.of(
                        errorCode.code(),
                        errorCode.message()
                );

        return ResponseEntity
                .status(errorCode.status())
                .body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception
    ) {
        ErrorCode errorCode =
                exception.getErrorCode();

        ErrorResponse response =
                ErrorResponse.of(
                        errorCode.code(),
                        errorCode.message()
                );

        return ResponseEntity
                .status(errorCode.status())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        ErrorCode errorCode =
                ErrorCode.VALIDATION_ERROR;

        List<FieldErrorResponse> fields =
                exception
                        .getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::toFieldErrorResponse)
                        .toList();

        ErrorResponse response =
                ErrorResponse.validation(
                        errorCode.code(),
                        errorCode.message(),
                        fields
                );

        return ResponseEntity
                .status(errorCode.status())
                .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleRequestBodyInvalid(
            HttpMessageNotReadableException exception
    ) {
        ErrorCode errorCode =
                ErrorCode.REQUEST_BODY_INVALID;

        ErrorResponse response =
                ErrorResponse.of(
                        errorCode.code(),
                        errorCode.message()
                );

        return ResponseEntity
                .status(errorCode.status())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        log.error(
                "처리되지 않은 서버 예외가 발생했습니다.",
                exception
        );

        ErrorCode errorCode =
                ErrorCode.INTERNAL_SERVER_ERROR;

        ErrorResponse response =
                ErrorResponse.of(
                        errorCode.code(),
                        errorCode.message()
                );

        return ResponseEntity
                .status(errorCode.status())
                .body(response);
    }

    private FieldErrorResponse toFieldErrorResponse(
            FieldError fieldError
    ) {
        String reason =
                fieldError.getDefaultMessage();

        if (reason == null || reason.isBlank()) {
            reason = "잘못된 입력값입니다.";
        }

        return new FieldErrorResponse(
                fieldError.getField(),
                reason
        );
    }
}

