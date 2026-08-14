package org.likelionhsu.hackathon.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.support.EmailVerificationCodeSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

@ActiveProfiles("test")
@SpringBootTest(properties =
        "app.auth.cookie.name=test_refresh_token")
@AutoConfigureMockMvc
@Import(AuthApiIntegrationTest.TestSenderConfig.class)
class AuthApiIntegrationTest {

    private static final String TRUSTED_ORIGIN =
            "http://localhost:3000";
    private static final String REFRESH_COOKIE_NAME =
            "test_refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CapturingEmailVerificationCodeSender codeSender;

    @Test
    void 이메일인증_회원가입_로그인_재발급_로그아웃_흐름() throws Exception {
        String email = "postman@example.com";

        mockMvc.perform(
                        post("/api/auth/email-verifications")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "postman@example.com",
                                          "purpose": "SIGNUP"
                                        }
                                        """)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.expiresInSeconds")
                        .value(300));

        String verificationCode = codeSender.lastCode();
        assertThat(verificationCode).matches("^[0-9]{6}$");

        MvcResult confirmResult = mockMvc.perform(
                        post("/api/auth/email-verifications/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "purpose": "SIGNUP",
                                          "verificationCode": "%s"
                                        }
                                        """.formatted(
                                        email,
                                        verificationCode
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresInSeconds")
                        .value(1200))
                .andReturn();

        String signupToken = JsonPath.read(
                confirmResult.getResponse().getContentAsString(),
                "$.data.signupToken"
        );

        mockMvc.perform(
                        get("/api/auth/login-ids/user_1234/availability")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));

        MvcResult signupResult = mockMvc.perform(
                        post("/api/auth/signup")
                                .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "signupToken": "%s",
                                          "loginId": "user_1234",
                                          "password": "password123",
                                          "passwordConfirm": "password123",
                                          "termsAgreements": [
                                            {
                                              "termsType": "SERVICE_TERMS",
                                              "termsVersion": "2026-08-01",
                                              "agreed": true
                                            },
                                            {
                                              "termsType": "PRIVACY_POLICY",
                                              "termsVersion": "2026-08-01",
                                              "agreed": true
                                            },
                                            {
                                              "termsType": "EMAIL_MARKETING",
                                              "termsVersion": "2026-08-01",
                                              "agreed": false
                                            }
                                          ],
                                          "nickname": "오늘뭐입지",
                                          "gender": "NOT_SPECIFIED"
                                        }
                                        """.formatted(signupToken))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds")
                        .value(1800))
                .andExpect(jsonPath("$.data.user.nickname")
                        .value("오늘뭐입지"))
                .andReturn();

        Cookie signupRefreshCookie = signupResult
                .getResponse()
                .getCookie(REFRESH_COOKIE_NAME);
        assertThat(signupRefreshCookie).isNotNull();
        assertThat(signupRefreshCookie.isHttpOnly()).isTrue();

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "loginId": "user_1234",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();

        String accessToken = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.data.accessToken"
        );
        Cookie loginRefreshCookie = loginResult
                .getResponse()
                .getCookie(REFRESH_COOKIE_NAME);
        assertThat(loginRefreshCookie).isNotNull();

        MvcResult refreshResult = mockMvc.perform(
                        post("/api/auth/refresh")
                                .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                                .cookie(loginRefreshCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andReturn();

        Cookie rotatedRefreshCookie = refreshResult
                .getResponse()
                .getCookie(REFRESH_COOKIE_NAME);
        assertThat(rotatedRefreshCookie).isNotNull();
        assertThat(rotatedRefreshCookie.getValue())
                .isNotEqualTo(loginRefreshCookie.getValue());

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                                .cookie(loginRefreshCookie)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("REFRESH_TOKEN_INVALID"));

        mockMvc.perform(
                        post("/api/auth/logout")
                                .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .cookie(rotatedRefreshCookie)
                )
                .andExpect(status().isNoContent())
                .andExpect(result -> {
                    Cookie clearedCookie = result
                            .getResponse()
                            .getCookie(REFRESH_COOKIE_NAME);
                    assertThat(clearedCookie).isNotNull();
                    assertThat(clearedCookie.getMaxAge()).isZero();
                });
    }

    @Test
    void 보호된_인증_API는_허용된_Origin이_필요하다() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "loginId": "user_1234",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("ORIGIN_NOT_ALLOWED"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSenderConfig {

        @Bean
        @Primary
        CapturingEmailVerificationCodeSender capturingSender() {
            return new CapturingEmailVerificationCodeSender();
        }
    }

    static class CapturingEmailVerificationCodeSender implements
            EmailVerificationCodeSender {

        private final AtomicReference<String> lastCode =
                new AtomicReference<>();

        @Override
        public void send(String email, String verificationCode) {
            lastCode.set(verificationCode);
        }

        String lastCode() {
            return lastCode.get();
        }
    }
}
