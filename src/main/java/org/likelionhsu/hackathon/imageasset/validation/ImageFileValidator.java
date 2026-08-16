package org.likelionhsu.hackathon.imageasset.validation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageFileValidator {

    public static final long MAX_FILE_SIZE =
            10L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png"
            );

    public ValidatedImageFile validate(
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw failure(
                    Failure.INVALID_FILE,
                    "이미지 파일이 비어 있습니다."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw failure(
                    Failure.FILE_TOO_LARGE,
                    "이미지 파일은 10MB 이하여야 합니다."
            );
        }

        String contentType = normalizeContentType(
                file.getContentType()
        );

        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw failure(
                    Failure.UNSUPPORTED_FORMAT,
                    "JPEG 또는 PNG 이미지만 업로드할 수 있습니다."
            );
        }

        byte[] bytes = readBytes(file);

        DetectedFormat detectedFormat =
                detectFormat(bytes);

        if (detectedFormat == null) {
            throw failure(
                    Failure.INVALID_FILE,
                    "올바른 이미지 파일이 아닙니다."
            );
        }

        if (!detectedFormat.contentType()
                .equals(contentType)) {
            throw failure(
                    Failure.INVALID_FILE,
                    "이미지 형식과 Content-Type이 일치하지 않습니다."
            );
        }

        BufferedImage image = decodeImage(bytes);

        if (image == null
                || image.getWidth() <= 0
                || image.getHeight() <= 0) {
            throw failure(
                    Failure.INVALID_FILE,
                    "이미지 데이터를 읽을 수 없습니다."
            );
        }

        return new ValidatedImageFile(
                bytes,
                detectedFormat.format(),
                image.getWidth(),
                image.getHeight()
        );
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();

            if (bytes.length == 0
                    || bytes.length > MAX_FILE_SIZE) {
                Failure failure = bytes.length
                        > MAX_FILE_SIZE
                        ? Failure.FILE_TOO_LARGE
                        : Failure.INVALID_FILE;

                throw failure(
                        failure,
                        failure == Failure.FILE_TOO_LARGE
                                ? "이미지 파일은 10MB 이하여야 합니다."
                                : "이미지 파일이 비어 있습니다."
                );
            }

            return bytes;
        } catch (ValidationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ValidationException(
                    Failure.INVALID_FILE,
                    "이미지 파일을 읽을 수 없습니다.",
                    exception
            );
        }
    }

    private BufferedImage decodeImage(byte[] bytes) {
        try (
                ByteArrayInputStream input =
                        new ByteArrayInputStream(bytes)
        ) {
            return ImageIO.read(input);
        } catch (IOException exception) {
            throw new ValidationException(
                    Failure.INVALID_FILE,
                    "이미지 데이터를 읽을 수 없습니다.",
                    exception
            );
        }
    }

    private DetectedFormat detectFormat(byte[] bytes) {
        if (isJpeg(bytes)) {
            return new DetectedFormat(
                    "jpg",
                    "image/jpeg"
            );
        }

        if (isPng(bytes)) {
            return new DetectedFormat(
                    "png",
                    "image/png"
            );
        }

        return null;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && unsigned(bytes[0]) == 0xFF
                && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        int[] signature = {
                0x89,
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A
        };

        if (bytes.length < signature.length) {
            return false;
        }

        for (int index = 0;
             index < signature.length;
             index++) {
            if (unsigned(bytes[index])
                    != signature[index]) {
                return false;
            }
        }

        return true;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private String normalizeContentType(
            String contentType
    ) {
        return contentType == null
                ? ""
                : contentType
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private ValidationException failure(
            Failure failure,
            String message
    ) {
        return new ValidationException(
                failure,
                message
        );
    }

    private record DetectedFormat(
            String format,
            String contentType
    ) {
    }

    public enum Failure {
        INVALID_FILE,
        FILE_TOO_LARGE,
        UNSUPPORTED_FORMAT
    }

    public static final class ValidationException
            extends RuntimeException {

        private final Failure failure;

        public ValidationException(
                Failure failure,
                String message
        ) {
            super(message);
            this.failure = failure;
        }

        public ValidationException(
                Failure failure,
                String message,
                Throwable cause
        ) {
            super(message, cause);
            this.failure = failure;
        }

        public Failure failure() {
            return failure;
        }
    }
}
