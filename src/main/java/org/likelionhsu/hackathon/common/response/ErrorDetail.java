package org.likelionhsu.hackathon.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ErrorDetail(
        String code,
        String message,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<FieldErrorResponse> fields
) {

    public ErrorDetail {
        if (fields != null) {
            fields = List.copyOf(fields);
        }
    }

    public static ErrorDetail of(
            String code,
            String message
    ) {
        return new ErrorDetail(
                code,
                message,
                null
        );
    }

    public static ErrorDetail withFields(
            String code,
            String message,
            List<FieldErrorResponse> fields
    ) {
        return new ErrorDetail(
                code,
                message,
                fields
        );
    }
}
