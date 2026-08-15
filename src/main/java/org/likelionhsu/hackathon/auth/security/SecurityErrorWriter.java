package org.likelionhsu.hackathon.auth.security;

import java.io.IOException;

import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityErrorWriter {

    public void write(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        response.setStatus(errorCode.status().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"error\":{"
                        + "\"code\":\"" + errorCode.code() + "\","
                        + "\"message\":\"" + errorCode.message() + "\"}}"
        );
    }
}
