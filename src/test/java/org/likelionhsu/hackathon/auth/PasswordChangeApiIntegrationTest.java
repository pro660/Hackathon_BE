package org.likelionhsu.hackathon.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.LocalCredential;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.LocalCredentialRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.auth.service.AuthTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordChangeApiIntegrationTest {

    private static final String TRUSTED_ORIGIN =
            "http://localhost:3000";
    private static final String LOGIN_ID = "password_user";
    private static final String CURRENT_PASSWORD = "password123";
    private static final String NEW_PASSWORD = "newPassword123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LocalCredentialRepository localCredentialRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthTokenService authTokenService;

    private String accessToken;
    private Cookie refreshCookie;

    @BeforeEach
    void setUp() throws Exception {
        User user = userRepository.saveAndFlush(
                User.local(
                        "password-change@example.com",
                        "비밀번호사용자",
                        Gender.NOT_SPECIFIED
                )
        );
        localCredentialRepository.saveAndFlush(
                new LocalCredential(
                        user,
                        LOGIN_ID,
                        passwordEncoder.encode(CURRENT_PASSWORD)
                )
        );

        MvcResult loginResult = login(
                LOGIN_ID,
                CURRENT_PASSWORD
        )
                .andExpect(status().isOk())
                .andReturn();

        accessToken = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.data.accessToken"
        );
        refreshCookie = loginResult.getResponse()
                .getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();
    }

    @Test
    void passwordChangeKeepsSessionAndSwitchesLoginPassword()
            throws Exception {
        mockMvc.perform(
                        patch("/api/users/me/password")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(passwordChangeJson(
                                        CURRENT_PASSWORD,
                                        NEW_PASSWORD,
                                        NEW_PASSWORD
                                ))
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        LocalCredential changedCredential = localCredentialRepository
                .findWithUserByLoginId(LOGIN_ID)
                .orElseThrow();
        assertThat(changedCredential.getPasswordHash())
                .isNotEqualTo(NEW_PASSWORD);
        assertThat(passwordEncoder.matches(
                NEW_PASSWORD,
                changedCredential.getPasswordHash()
        )).isTrue();

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .header(
                                        HttpHeaders.ORIGIN,
                                        TRUSTED_ORIGIN
                                )
                                .cookie(refreshCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());

        login(LOGIN_ID, CURRENT_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("INVALID_CREDENTIALS"));

        login(LOGIN_ID, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    void unauthenticatedPasswordChangeReturns401() throws Exception {
        mockMvc.perform(
                        patch("/api/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(passwordChangeJson(
                                        CURRENT_PASSWORD,
                                        NEW_PASSWORD,
                                        NEW_PASSWORD
                                ))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongCurrentPasswordReturns400() throws Exception {
        mockMvc.perform(
                        passwordChangeRequest(passwordChangeJson(
                                "wrongPassword123",
                                NEW_PASSWORD,
                                NEW_PASSWORD
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("CURRENT_PASSWORD_MISMATCH"));
    }

    @Test
    void passwordConfirmationMismatchReturns400() throws Exception {
        mockMvc.perform(
                        passwordChangeRequest(passwordChangeJson(
                                CURRENT_PASSWORD,
                                NEW_PASSWORD,
                                "differentPassword123"
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("PASSWORD_CONFIRM_MISMATCH"));
    }

    @Test
    void samePasswordAsCurrentReturns400() throws Exception {
        mockMvc.perform(
                        passwordChangeRequest(passwordChangeJson(
                                CURRENT_PASSWORD,
                                CURRENT_PASSWORD,
                                CURRENT_PASSWORD
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("NEW_PASSWORD_SAME_AS_CURRENT"));
    }

    @Test
    void passwordShorterThanEightCharactersReturnsValidationError()
            throws Exception {
        assertInvalidNewPassword("abc1234");
    }

    @Test
    void passwordWithoutLetterReturnsValidationError()
            throws Exception {
        assertInvalidNewPassword("12345678");
    }

    @Test
    void passwordWithoutNumberReturnsValidationError()
            throws Exception {
        assertInvalidNewPassword("abcdefgh");
    }

    @Test
    void passwordLongerThanSixtyFourCharactersReturnsValidationError()
            throws Exception {
        assertInvalidNewPassword("a".repeat(64) + "1");
    }

    @Test
    void accountWithoutLocalCredentialReturns409() throws Exception {
        User socialUser = userRepository.saveAndFlush(
                User.social(
                        "소셜사용자",
                        Gender.NOT_SPECIFIED,
                        null
                )
        );
        String socialAccessToken = authTokenService
                .issue(socialUser)
                .accessToken();

        mockMvc.perform(
                        patch("/api/users/me/password")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(socialAccessToken)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(passwordChangeJson(
                                        CURRENT_PASSWORD,
                                        NEW_PASSWORD,
                                        NEW_PASSWORD
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code")
                        .value(
                                "PASSWORD_CHANGE_NOT_AVAILABLE"
                        ));
    }

    @Test
    void inactiveUserReturns403() throws Exception {
        User inactiveUser = userRepository.saveAndFlush(
                User.local(
                        "inactive-password@example.com",
                        "비활성사용자",
                        Gender.NOT_SPECIFIED
                )
        );
        localCredentialRepository.saveAndFlush(
                new LocalCredential(
                        inactiveUser,
                        "inactive_user",
                        passwordEncoder.encode(CURRENT_PASSWORD)
                )
        );
        String inactiveAccessToken = authTokenService
                .issue(inactiveUser)
                .accessToken();
        inactiveUser.beginDeletion();
        userRepository.saveAndFlush(inactiveUser);

        mockMvc.perform(
                        patch("/api/users/me/password")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(inactiveAccessToken)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(passwordChangeJson(
                                        CURRENT_PASSWORD,
                                        NEW_PASSWORD,
                                        NEW_PASSWORD
                                ))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("ACCOUNT_NOT_ACTIVE"));
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String loginId,
            String password
    ) throws Exception {
        return mockMvc.perform(
                post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "%s",
                                  "password": "%s"
                                }
                                """.formatted(loginId, password))
        );
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    passwordChangeRequest(String content) {
        return patch("/api/users/me/password")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        bearer(accessToken)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }

    private String passwordChangeJson(
            String currentPassword,
            String newPassword,
            String newPasswordConfirm
    ) {
        return """
                {
                  "currentPassword": "%s",
                  "newPassword": "%s",
                  "newPasswordConfirm": "%s"
                }
                """.formatted(
                currentPassword,
                newPassword,
                newPasswordConfirm
        );
    }

    private void assertInvalidNewPassword(String newPassword)
            throws Exception {
        mockMvc.perform(
                        passwordChangeRequest(passwordChangeJson(
                                CURRENT_PASSWORD,
                                newPassword,
                                newPassword
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field")
                        .value("newPassword"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
