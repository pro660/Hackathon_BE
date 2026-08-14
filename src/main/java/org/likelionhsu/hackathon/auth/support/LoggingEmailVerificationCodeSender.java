package org.likelionhsu.hackathon.auth.support;

import org.likelionhsu.hackathon.auth.config.AuthProperties;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailVerificationCodeSender implements
        EmailVerificationCodeSender {

    private static final Logger log = LoggerFactory.getLogger(
            LoggingEmailVerificationCodeSender.class
    );

    private final AuthProperties properties;

    public LoggingEmailVerificationCodeSender(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(String email, String verificationCode) {
        if (properties.logVerificationCode()) {
            log.info(
                    "[LOCAL EMAIL VERIFICATION] email={}, code={}",
                    email,
                    verificationCode
            );
            return;
        }

        log.warn(
                "이메일 발송 Provider가 연결되지 않았습니다. email={}",
                email
        );
        throw new BusinessException(ErrorCode.EMAIL_PROVIDER_UNAVAILABLE);
    }
}
