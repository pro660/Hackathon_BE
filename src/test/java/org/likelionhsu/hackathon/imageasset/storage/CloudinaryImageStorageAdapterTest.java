package org.likelionhsu.hackathon.imageasset.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CloudinaryImageStorageAdapterTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private CloudinaryImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);

        when(cloudinary.uploader())
                .thenReturn(uploader);

        adapter =
                new CloudinaryImageStorageAdapter(
                        cloudinary
                );
    }

    @Test
    void uploadMapsCloudinaryMetadata()
            throws Exception {
        when(uploader.upload(
                any(byte[].class),
                anyMap()
        )).thenReturn(
                Map.of(
                        "public_id",
                        "wear-it/user-items/asset-1",
                        "secure_url",
                        "https://res.cloudinary.com/demo/image/upload/asset-1.jpg",
                        "format",
                        "jpg",
                        "bytes",
                        1234L,
                        "width",
                        640,
                        "height",
                        480
                )
        );

        StoredImage result = adapter.upload(
                new ImageStorageUploadRequest(
                        new byte[] {1, 2, 3},
                        "asset-1"
                )
        );

        assertThat(result.publicId())
                .isEqualTo(
                        "wear-it/user-items/asset-1"
                );
        assertThat(result.secureUrl())
                .startsWith("https://");
        assertThat(result.format())
                .isEqualTo("jpg");
        assertThat(result.bytes())
                .isEqualTo(1234L);
        assertThat(result.width())
                .isEqualTo(640);
        assertThat(result.height())
                .isEqualTo(480);

        verify(uploader).upload(
                any(byte[].class),
                anyMap()
        );
    }

    @Test
    void uploadFailureIsWrapped()
            throws Exception {
        when(uploader.upload(
                any(byte[].class),
                anyMap()
        )).thenThrow(
                new IOException("cloudinary unavailable")
        );

        assertThatThrownBy(
                () -> adapter.upload(
                        new ImageStorageUploadRequest(
                                new byte[] {1, 2, 3},
                                "asset-1"
                        )
                )
        )
                .isInstanceOf(
                        ImageStorageException.class
                )
                .hasMessageContaining("업로드");
    }

    @Test
    void deleteAcceptsOkResult()
            throws Exception {
        when(uploader.destroy(
                eq("wear-it/user-items/asset-1"),
                anyMap()
        )).thenReturn(
                Map.of("result", "ok")
        );

        adapter.delete(
                "wear-it/user-items/asset-1"
        );

        verify(uploader).destroy(
                eq("wear-it/user-items/asset-1"),
                anyMap()
        );
    }

    @Test
    void deleteTreatsAlreadyMissingAssetAsSuccess()
            throws Exception {
        when(uploader.destroy(
                eq("wear-it/user-items/asset-1"),
                anyMap()
        )).thenReturn(
                Map.of("result", "not found")
        );

        adapter.delete(
                "wear-it/user-items/asset-1"
        );
    }

    @Test
    void malformedUploadResponseIsRejected() {
        assertThatThrownBy(
                () -> adapter.toStoredImage(
                        Map.of(
                                "public_id",
                                "asset-1"
                        )
                )
        )
                .isInstanceOf(
                        ImageStorageException.class
                );
    }
}
