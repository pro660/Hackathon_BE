package org.likelionhsu.hackathon.common.response;

public record ApiResponse<T>(
        boolean success,
        T data
) {

    public ApiResponse {
        if (!success) {
            throw new IllegalArgumentException(
                    "성공 응답의 success는 true여야 합니다."
            );
        }
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                data
        );
    }
}
