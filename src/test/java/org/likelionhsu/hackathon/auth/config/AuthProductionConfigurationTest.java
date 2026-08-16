package org.likelionhsu.hackathon.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AuthProductionConfigurationTest {

    @Test
    void 운영_JWT_Secret은_환경변수를_필수로_사용한다() throws IOException {
        Properties properties = new Properties();
        try (var input = new ClassPathResource(
                "application-prod.properties"
        ).getInputStream()) {
            properties.load(input);
        }

        assertThat(properties.getProperty("app.auth.jwt-secret"))
                .isEqualTo("${JWT_SECRET}");
        assertThat(properties.getProperty(
                "app.auth.oauth.naver.client-secret"
        )).isEqualTo("${NAVER_CLIENT_SECRET}");
        assertThat(properties.getProperty(
                "app.auth.oauth.kakao.client-secret"
        )).isEqualTo("${KAKAO_CLIENT_SECRET}");
        assertThat(properties.getProperty(
                "app.auth.oauth.success-url"
        )).isEqualTo("${FRONTEND_OAUTH_SUCCESS_URL}");
    }
}
