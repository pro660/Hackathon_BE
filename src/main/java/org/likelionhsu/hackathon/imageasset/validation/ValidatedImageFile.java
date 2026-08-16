package org.likelionhsu.hackathon.imageasset.validation;

import java.util.Objects;

public record ValidatedImageFile(
        byte[] bytes,
        String format,
        int width,
        int height
) {

    public ValidatedImageFile {
        Objects.requireNonNull(
                bytes,
                "bytes는 null일 수 없습니다."
        );

        if (bytes.length == 0) {
            throw new IllegalArgumentException(
                    "bytes는 비어 있을 수 없습니다."
            );
        }

        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException(
                    "format은 비어 있을 수 없습니다."
            );
        }

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "이미지 크기는 0보다 커야 합니다."
            );
        }

        bytes = bytes.clone();
        format = format.trim();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
