package org.likelionhsu.hackathon.auth.config;

import java.time.Duration;

import org.likelionhsu.hackathon.auth.domain.SocialProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.oauth")
public record OAuthProperties(
        String successUrl,
        String onboardingUrl,
        Duration stateTtl,
        Duration onboardingTokenTtl,
        Cookie stateCookie,
        Cookie onboardingCookie,
        Provider naver,
        Provider kakao
) {

    public Provider provider(SocialProvider provider) {
        return switch (provider) {
            case NAVER -> naver;
            case KAKAO -> kakao;
        };
    }

    public record Cookie(
            String name,
            String path,
            String sameSite,
            boolean secure
    ) {
    }

    public record Provider(
            String clientId,
            String clientSecret,
            String redirectUri,
            String authorizationUri,
            String tokenUri,
            String profileUri
    ) {
    }
}
