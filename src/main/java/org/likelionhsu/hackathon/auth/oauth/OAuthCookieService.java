package org.likelionhsu.hackathon.auth.oauth;

import java.time.Duration;

import org.likelionhsu.hackathon.auth.config.OAuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class OAuthCookieService {

    private final OAuthProperties properties;

    public OAuthCookieService(OAuthProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie createState(String state) {
        return cookie(
                properties.stateCookie(),
                state,
                properties.stateTtl()
        );
    }

    public ResponseCookie clearState() {
        return cookie(
                properties.stateCookie(),
                "",
                Duration.ZERO
        );
    }

    public String readState(HttpServletRequest request) {
        return read(request, properties.stateCookie().name());
    }

    public ResponseCookie createOnboarding(String token) {
        return cookie(
                properties.onboardingCookie(),
                token,
                properties.onboardingTokenTtl()
        );
    }

    public ResponseCookie clearOnboarding() {
        return cookie(
                properties.onboardingCookie(),
                "",
                Duration.ZERO
        );
    }

    public String readOnboarding(HttpServletRequest request) {
        return read(request, properties.onboardingCookie().name());
    }

    private String read(HttpServletRequest request, String cookieName) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie cookie(
            OAuthProperties.Cookie cookie,
            String value,
            Duration maxAge
    ) {
        return ResponseCookie.from(cookie.name(), value)
                .httpOnly(true)
                .secure(cookie.secure())
                .sameSite(cookie.sameSite())
                .path(cookie.path())
                .maxAge(maxAge)
                .build();
    }
}
