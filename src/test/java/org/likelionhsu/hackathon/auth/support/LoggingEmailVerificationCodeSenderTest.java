package org.likelionhsu.hackathon.auth.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.config.AuthProperties;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;

class LoggingEmailVerificationCodeSenderTest {

    @Test
    void 인증번호_로그가_비활성화되면_발송_성공으로_처리하지_않는다() {
        LoggingEmailVerificationCodeSender sender =
                new LoggingEmailVerificationCodeSender(properties(false));

        assertThatThrownBy(() ->
                sender.send("user@example.com", "123456")
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        ErrorCode.EMAIL_PROVIDER_UNAVAILABLE
                                )
                );
    }

    private AuthProperties properties(boolean logVerificationCode) {
        return new AuthProperties(
                "hackathon-be",
                "test-secret-key-that-is-at-least-32-characters",
                Duration.ofMinutes(30),
                Duration.ofDays(14),
                Duration.ofMinutes(5),
                Duration.ofMinutes(20),
                Duration.ofMinutes(1),
                10,
                5,
                logVerificationCode,
                new AuthProperties.Cookie(
                        "refresh_token",
                        "/api/auth",
                        "Lax",
                        false
                )
        );
    }
}
