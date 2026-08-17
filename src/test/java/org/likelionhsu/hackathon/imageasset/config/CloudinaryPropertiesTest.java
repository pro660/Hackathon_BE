package org.likelionhsu.hackathon.imageasset.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class CloudinaryPropertiesTest {

    @Test
    void cloudinaryPropertiesBindFromEnvironment() {
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty(
                                "app.image-storage.cloudinary.cloud-name",
                                "demo-cloud"
                        )
                        .withProperty(
                                "app.image-storage.cloudinary.api-key",
                                "demo-key"
                        )
                        .withProperty(
                                "app.image-storage.cloudinary.api-secret",
                                "demo-secret"
                        );

        CloudinaryProperties properties =
                Binder.get(environment)
                        .bind(
                                "app.image-storage.cloudinary",
                                CloudinaryProperties.class
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Cloudinary 설정 바인딩에 실패했습니다."
                                ));

        assertThat(properties.cloudName())
                .isEqualTo("demo-cloud");
        assertThat(properties.apiKey())
                .isEqualTo("demo-key");
        assertThat(properties.apiSecret())
                .isEqualTo("demo-secret");
        assertThat(properties.isConfigured())
                .isTrue();
    }

    @Test
    void blankCredentialsAreNotConfigured() {
        CloudinaryProperties properties =
                new CloudinaryProperties(
                        " ",
                        null,
                        ""
                );

        assertThat(properties.isConfigured())
                .isFalse();
        assertThat(properties.cloudName())
                .isEmpty();
        assertThat(properties.apiKey())
                .isEmpty();
        assertThat(properties.apiSecret())
                .isEmpty();
    }
}
