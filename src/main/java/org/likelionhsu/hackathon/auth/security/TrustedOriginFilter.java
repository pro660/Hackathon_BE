package org.likelionhsu.hackathon.auth.security;

import java.io.IOException;
import java.util.Set;

import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TrustedOriginFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/oauth/signup",
            "/api/auth/reauthentications",
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    private final String trustedOrigin;
    private final SecurityErrorWriter errorWriter;

    public TrustedOriginFilter(
            @Value("${app.cors.allowed-origin}") String trustedOrigin,
            SecurityErrorWriter errorWriter
    ) {
        this.trustedOrigin = trustedOrigin;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader("Origin");

        if (!trustedOrigin.equals(origin)) {
            errorWriter.write(response, ErrorCode.ORIGIN_NOT_ALLOWED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
