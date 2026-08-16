package org.likelionhsu.hackathon.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.common.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

class GlobalExceptionHandlerImageUploadTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void maxUploadSizeReturns413ImageFileTooLarge() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMaxUploadSizeExceeded(
                        new MaxUploadSizeExceededException(
                                10L * 1024L * 1024L
                        )
                );

        assertThat(response.getStatusCode().value())
                .isEqualTo(413);
        assertThat(response.getBody())
                .isNotNull();
        assertThat(response.getBody().error().code())
                .isEqualTo("IMAGE_FILE_TOO_LARGE");
    }

    @Test
    void missingMultipartFileReturns400ImageFileInvalid() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMissingServletRequestPart(
                        new MissingServletRequestPartException(
                                "file"
                        )
                );

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);
        assertThat(response.getBody())
                .isNotNull();
        assertThat(response.getBody().error().code())
                .isEqualTo("IMAGE_FILE_INVALID");
    }
}
