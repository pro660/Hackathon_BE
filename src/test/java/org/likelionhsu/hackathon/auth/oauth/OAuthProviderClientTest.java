package org.likelionhsu.hackathon.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.config.OAuthProperties;
import org.likelionhsu.hackathon.auth.domain.SocialProvider;

class OAuthProviderClientTest {

    @Test
    void 승인_URL의_Callback_주소를_Query_Value로_인코딩한다() {
        OAuthProviderClient client = new OAuthProviderClient(properties());

        URI uri = client.authorizationUri(
                SocialProvider.NAVER,
                "signed.state"
        );

        assertThat(uri.getRawQuery())
                .contains("response_type=code")
                .contains("client_id=naver-client")
                .contains("redirect_uri=https%3A%2F%2Ffrontend.example.com"
                        + "%2Fapi%2Fauth%2Foauth%2Fnaver%2Fcallback")
                .contains("state=signed.state");
    }

    private OAuthProperties properties() {
        OAuthProperties.Cookie stateCookie = new OAuthProperties.Cookie(
                "oauth_state",
                "/api/auth/oauth",
                "Lax",
                false
        );
        OAuthProperties.Cookie onboardingCookie =
                new OAuthProperties.Cookie(
                        "social_onboarding_token",
                        "/api/auth/oauth/signup",
                        "Lax",
                        false
                );
        OAuthProperties.Provider naver = new OAuthProperties.Provider(
                "naver-client",
                "naver-secret",
                "https://frontend.example.com/api/auth/oauth/naver/callback",
                "https://nid.naver.com/oauth2.0/authorize",
                "https://nid.naver.com/oauth2.0/token",
                "https://openapi.naver.com/v1/nid/me"
        );
        OAuthProperties.Provider kakao = new OAuthProperties.Provider(
                "kakao-client",
                "kakao-secret",
                "https://frontend.example.com/api/auth/oauth/kakao/callback",
                "https://kauth.kakao.com/oauth/authorize",
                "https://kauth.kakao.com/oauth/token",
                "https://kapi.kakao.com/v2/user/me"
        );
        return new OAuthProperties(
                "https://frontend.example.com/oauth/success",
                "https://frontend.example.com/oauth/onboarding",
                Duration.ofMinutes(5),
                Duration.ofMinutes(20),
                stateCookie,
                onboardingCookie,
                naver,
                kakao
        );
    }
}
