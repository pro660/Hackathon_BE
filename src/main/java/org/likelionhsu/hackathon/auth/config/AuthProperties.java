package org.likelionhsu.hackathon.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String issuer,
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration emailCodeTtl,
        Duration signupTokenTtl,
        Duration emailResendInterval,
        int emailDailySendLimit,
        int emailMaxAttempts,
        boolean logVerificationCode,
        Cookie cookie
) {

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET은 32자 이상이어야 합니다."
            );
        }
    }

    public record Cookie(
            String name,
            String path,
            String sameSite,
            boolean secure
    ) {
    }
}
