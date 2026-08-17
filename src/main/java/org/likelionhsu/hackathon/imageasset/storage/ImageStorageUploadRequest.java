package org.likelionhsu.hackathon.imageasset.storage;

import java.util.Objects;

public record ImageStorageUploadRequest(
        byte[] bytes,
        String publicId
) {

    public ImageStorageUploadRequest {
        Objects.requireNonNull(
                bytes,
                "bytes는 null일 수 없습니다."
        );

        if (bytes.length == 0) {
            throw new IllegalArgumentException(
                    "bytes는 비어 있을 수 없습니다."
            );
        }

        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException(
                    "publicId는 비어 있을 수 없습니다."
            );
        }

        bytes = bytes.clone();
        publicId = publicId.trim();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
