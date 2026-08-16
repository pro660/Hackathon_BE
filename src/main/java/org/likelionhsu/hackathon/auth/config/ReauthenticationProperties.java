package org.likelionhsu.hackathon.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.reauthentication")
public record ReauthenticationProperties(
        String successUrl,
        Duration tokenTtl,
        Cookie cookie
) {

    public record Cookie(
            String name,
            String path,
            String sameSite,
            boolean secure
    ) {
    }
}
