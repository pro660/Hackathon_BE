package org.likelionhsu.hackathon.auth.support;

public interface EmailVerificationCodeSender {

    void send(String email, String verificationCode);
}
