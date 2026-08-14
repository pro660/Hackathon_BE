package org.likelionhsu.hackathon.auth.security;

import java.io.IOException;

import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter errorWriter;

    public RestAuthenticationEntryPoint(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        ErrorCode errorCode = isExpired(exception)
                ? ErrorCode.ACCESS_TOKEN_EXPIRED
                : ErrorCode.ACCESS_TOKEN_INVALID;
        errorWriter.write(response, errorCode);
    }

    private boolean isExpired(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase(java.util.Locale.ROOT)
                    .contains("expired")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
