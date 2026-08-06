package org.likelionhsu.hackathon.common.response;

public record FieldErrorResponse(
        String field,
        String reason
) {
}
