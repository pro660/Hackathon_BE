package org.likelionhsu.hackathon.auth.security;

import java.io.IOException;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ActiveUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final SecurityErrorWriter errorWriter;

    public ActiveUserFilter(
            UserRepository userRepository,
            SecurityErrorWriter errorWriter
    ) {
        this.userRepository = userRepository;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwt)
                || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(jwt.getToken().getSubject());
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            errorWriter.write(
                    response,
                    ErrorCode.ACCESS_TOKEN_INVALID
            );
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            SecurityContextHolder.clearContext();
            errorWriter.write(
                    response,
                    ErrorCode.ACCESS_TOKEN_INVALID
            );
            return;
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            SecurityContextHolder.clearContext();
            errorWriter.write(
                    response,
                    ErrorCode.ACCOUNT_NOT_ACTIVE
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
