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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MissingServletRequestParameterException;

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

    @ExceptionHandler(RequestValidationException.class)
    public ResponseEntity<ErrorResponse> handleRequestValidation(
            RequestValidationException exception
    ) {
        ErrorCode errorCode =
                ErrorCode.VALIDATION_ERROR;

        FieldErrorResponse field =
                new FieldErrorResponse(
                        exception.getField(),
                        exception.getReason()
                );

        ErrorResponse response =
                ErrorResponse.validation(
                        errorCode.code(),
                        errorCode.message(),
                        List.of(field)
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

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException exception
    ) {
        ErrorCode errorCode =
                ErrorCode.VALIDATION_ERROR;

        List<FieldErrorResponse> fields =
                exception
                        .getParameterValidationResults()
                        .stream()
                        .flatMap(result -> {
                            String field =
                                    result
                                            .getMethodParameter()
                                            .getParameterName();

                            if (field == null || field.isBlank()) {
                                field = "parameter";
                            }

                            String resolvedField = field;

                            return result
                                    .getResolvableErrors()
                                    .stream()
                                    .map(error -> {
                                        String reason =
                                                error.getDefaultMessage();

                                        if (reason == null
                                                || reason.isBlank()) {
                                            reason =
                                                    "잘못된 입력값입니다.";
                                        }

                                        return new FieldErrorResponse(
                                                resolvedField,
                                                reason
                                        );
                                    });
                        })
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        ErrorCode errorCode =
                ErrorCode.VALIDATION_ERROR;

        FieldErrorResponse field =
                new FieldErrorResponse(
                        exception.getName(),
                        "잘못된 입력값입니다."
                );

        ErrorResponse response =
                ErrorResponse.validation(
                        errorCode.code(),
                        errorCode.message(),
                        List.of(field)
                );

        return ResponseEntity
                .status(errorCode.status())
                .body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        ErrorCode errorCode =
                ErrorCode.VALIDATION_ERROR;

        FieldErrorResponse field =
                new FieldErrorResponse(
                        exception.getParameterName(),
                        "필수 입력값입니다."
                );

        ErrorResponse response =
                ErrorResponse.validation(
                        errorCode.code(),
                        errorCode.message(),
                        List.of(field)
                );

        return ResponseEntity
                .status(errorCode.status())
                .body(response);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(
            MissingServletRequestPartException exception
    ) {
        ErrorCode errorCode =
                ErrorCode.IMAGE_FILE_INVALID;

        ErrorResponse response =
                ErrorResponse.of(
                        errorCode.code(),
                        errorCode.message()
                );

        return ResponseEntity
                .status(errorCode.status())
                .body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception
    ) {
        ErrorCode errorCode =
                ErrorCode.IMAGE_FILE_TOO_LARGE;

        ErrorResponse response =
                ErrorResponse.of(
                        errorCode.code(),
                        errorCode.message()
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

