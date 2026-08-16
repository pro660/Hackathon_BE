package org.likelionhsu.hackathon.auth.support;

import java.time.Duration;

import org.likelionhsu.hackathon.auth.config.ReauthenticationProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class ReauthenticationCookieService {

    private final ReauthenticationProperties properties;

    public ReauthenticationCookieService(
            ReauthenticationProperties properties
    ) {
        this.properties = properties;
    }

    public ResponseCookie create(String token) {
        return baseCookie(token)
                .maxAge(properties.tokenTtl())
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(
                request,
                properties.cookie().name()
        );
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(
            String value
    ) {
        ReauthenticationProperties.Cookie cookie =
                properties.cookie();

        return ResponseCookie.from(cookie.name(), value)
                .httpOnly(true)
                .secure(cookie.secure())
                .sameSite(cookie.sameSite())
                .path(cookie.path());
    }
}
