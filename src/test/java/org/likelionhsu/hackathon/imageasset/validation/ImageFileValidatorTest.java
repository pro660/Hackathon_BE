package org.likelionhsu.hackathon.imageasset.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageFileValidatorTest {

    private final ImageFileValidator validator =
            new ImageFileValidator();

    @Test
    void validJpegIsAccepted() throws Exception {
        byte[] bytes = imageBytes("jpg");

        ValidatedImageFile result = validator.validate(
                multipart(
                        "item.jpg",
                        "image/jpeg",
                        bytes
                )
        );

        assertThat(result.format()).isEqualTo("jpg");
        assertThat(result.width()).isEqualTo(2);
        assertThat(result.height()).isEqualTo(2);
        assertThat(result.bytes()).isEqualTo(bytes);
    }

    @Test
    void validPngIsAccepted() throws Exception {
        byte[] bytes = imageBytes("png");

        ValidatedImageFile result = validator.validate(
                multipart(
                        "item.png",
                        "image/png",
                        bytes
                )
        );

        assertThat(result.format()).isEqualTo("png");
        assertThat(result.width()).isEqualTo(2);
        assertThat(result.height()).isEqualTo(2);
    }

    @Test
    void emptyFileIsRejected() {
        assertFailure(
                multipart(
                        "empty.jpg",
                        "image/jpeg",
                        new byte[0]
                ),
                ImageFileValidator.Failure.INVALID_FILE
        );
    }

    @Test
    void fileOverTenMegabytesIsRejected() {
        assertFailure(
                multipart(
                        "large.jpg",
                        "image/jpeg",
                        new byte[
                                (int) ImageFileValidator
                                        .MAX_FILE_SIZE
                                        + 1
                        ]
                ),
                ImageFileValidator.Failure.FILE_TOO_LARGE
        );
    }

    @Test
    void gifIsRejected() {
        assertFailure(
                multipart(
                        "item.gif",
                        "image/gif",
                        "GIF89a".getBytes()
                ),
                ImageFileValidator.Failure.UNSUPPORTED_FORMAT
        );
    }

    @Test
    void svgIsRejected() {
        assertFailure(
                multipart(
                        "item.svg",
                        "image/svg+xml",
                        "<svg></svg>".getBytes()
                ),
                ImageFileValidator.Failure.UNSUPPORTED_FORMAT
        );
    }

    @Test
    void webpIsRejected() {
        assertFailure(
                multipart(
                        "item.webp",
                        "image/webp",
                        "RIFFxxxxWEBP".getBytes()
                ),
                ImageFileValidator.Failure.UNSUPPORTED_FORMAT
        );
    }

    @Test
    void spoofedJpegMimeTypeIsRejected() {
        assertFailure(
                multipart(
                        "fake.jpg",
                        "image/jpeg",
                        "not-an-image".getBytes()
                ),
                ImageFileValidator.Failure.INVALID_FILE
        );
    }

    @Test
    void corruptJpegIsRejected() {
        assertFailure(
                multipart(
                        "corrupt.jpg",
                        "image/jpeg",
                        new byte[] {
                                (byte) 0xFF,
                                (byte) 0xD8,
                                (byte) 0xFF,
                                0x00,
                                0x01
                        }
                ),
                ImageFileValidator.Failure.INVALID_FILE
        );
    }

    @Test
    void contentTypeAndBinaryFormatMustMatch()
            throws Exception {
        assertFailure(
                multipart(
                        "wrong.jpg",
                        "image/jpeg",
                        imageBytes("png")
                ),
                ImageFileValidator.Failure.INVALID_FILE
        );
    }

    private void assertFailure(
            MockMultipartFile file,
            ImageFileValidator.Failure expected
    ) {
        assertThatThrownBy(
                () -> validator.validate(file)
        )
                .isInstanceOf(
                        ImageFileValidator
                                .ValidationException.class
                )
                .satisfies(exception -> {
                    ImageFileValidator.ValidationException
                            validationException =
                            (ImageFileValidator
                                    .ValidationException)
                                    exception;

                    assertThat(
                            validationException.failure()
                    ).isEqualTo(expected);
                });
    }

    private MockMultipartFile multipart(
            String filename,
            String contentType,
            byte[] bytes
    ) {
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                bytes
        );
    }

    private byte[] imageBytes(String format)
            throws IOException {
        BufferedImage image =
                new BufferedImage(
                        2,
                        2,
                        BufferedImage.TYPE_INT_RGB
                );

        try (
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {
            boolean written = ImageIO.write(
                    image,
                    format,
                    output
            );

            if (!written) {
                throw new IllegalStateException(
                        "테스트 이미지 생성에 실패했습니다."
                );
            }

            return output.toByteArray();
        }
    }
}
