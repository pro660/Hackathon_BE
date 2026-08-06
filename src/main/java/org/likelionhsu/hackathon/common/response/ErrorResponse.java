package org.likelionhsu.hackathon.common.response;

import java.util.List;

public record ErrorResponse(
        boolean success,
        ErrorDetail error
) {

    public ErrorResponse {
        if (success) {
            throw new IllegalArgumentException(
                    "오류 응답의 success는 false여야 합니다."
            );
        }

        if (error == null) {
            throw new IllegalArgumentException(
                    "오류 응답에는 error가 필요합니다."
            );
        }
    }

    public static ErrorResponse of(
            String code,
            String message
    ) {
        return new ErrorResponse(
                false,
                ErrorDetail.of(code, message)
        );
    }

    public static ErrorResponse validation(
            String code,
            String message,
            List<FieldErrorResponse> fields
    ) {
        return new ErrorResponse(
                false,
                ErrorDetail.withFields(
                        code,
                        message,
                        fields
                )
        );
    }
}
