package org.likelionhsu.hackathon.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.SocialProvider;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.oauth.OAuthProfile;
import org.likelionhsu.hackathon.auth.oauth.OAuthProviderClient;
import org.likelionhsu.hackathon.auth.repository.EmailVerificationRepository;
import org.likelionhsu.hackathon.auth.repository.LocalCredentialRepository;
import org.likelionhsu.hackathon.auth.repository.PendingSocialSignupRepository;
import org.likelionhsu.hackathon.auth.repository.ReauthTokenRepository;
import org.likelionhsu.hackathon.auth.repository.RefreshTokenRepository;
import org.likelionhsu.hackathon.auth.repository.SocialAccountRepository;
import org.likelionhsu.hackathon.auth.repository.TermsAgreementRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "app.auth.cookie.name=test_refresh_token",
        "app.auth.oauth.success-url=http://localhost:3000/oauth/success",
        "app.auth.oauth.onboarding-url=http://localhost:3000/oauth/onboarding",
        "app.auth.oauth.state-cookie.name=test_oauth_state",
        "app.auth.oauth.onboarding-cookie.name=test_social_onboarding_token"
})
@AutoConfigureMockMvc
class OAuthApiIntegrationTest {

    private static final String TRUSTED_ORIGIN =
            "http://localhost:3000";
    private static final String STATE_COOKIE = "test_oauth_state";
    private static final String ONBOARDING_COOKIE =
            "test_social_onboarding_token";
    private static final String REFRESH_COOKIE = "test_refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthProviderClient providerClient;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ReauthTokenRepository reauthTokenRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private LocalCredentialRepository localCredentialRepository;

    @Autowired
    private TermsAgreementRepository termsAgreementRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private PendingSocialSignupRepository pendingRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        reauthTokenRepository.deleteAll();
        termsAgreementRepository.deleteAll();
        socialAccountRepository.deleteAll();
        emailVerificationRepository.deleteAll();
        localCredentialRepository.deleteAll();
        pendingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void OAuth_시작은_State_Cookie와_공급자_Redirect를_반환한다()
            throws Exception {
        when(providerClient.authorizationUri(
                eq(SocialProvider.NAVER),
                anyString()
        )).thenAnswer(invocation -> URI.create(
                "https://nid.naver.com/oauth2.0/authorize?state="
                        + invocation.getArgument(1, String.class)
        ));

        MvcResult result = startOAuth("naver");
        Cookie stateCookie = result.getResponse().getCookie(STATE_COOKIE);

        assertThat(stateCookie).isNotNull();
        assertThat(stateCookie.isHttpOnly()).isTrue();
        assertThat(stateCookie.getMaxAge()).isEqualTo(300);
        assertThat(stateCookie.getPath()).isEqualTo("/api/auth/oauth");
        assertThat(result.getResponse().getRedirectedUrl())
                .contains("https://nid.naver.com/oauth2.0/authorize")
                .contains("state=" + stateCookie.getValue());
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(value -> assertThat(value)
                        .contains("HttpOnly")
                        .contains("SameSite=Lax"));
    }

    @Test
    void 잘못된_State는_거부하고_State_Cookie를_삭제한다()
            throws Exception {
        mockMvc.perform(
                        get("/api/auth/oauth/kakao/callback")
                                .param("code", "authorization-code")
                                .param("state", "wrong-state")
                                .cookie(new Cookie(
                                        STATE_COOKIE,
                                        "cookie-state"
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("OAUTH_STATE_INVALID"))
                .andExpect(result -> {
                    Cookie cleared = result.getResponse()
                            .getCookie(STATE_COOKIE);
                    assertThat(cleared).isNotNull();
                    assertThat(cleared.getMaxAge()).isZero();
                });

        verifyNoInteractions(providerClient);
    }

    @Test
    void 신규_소셜_사용자는_Pending_온보딩_후_가입하고_재로그인한다()
            throws Exception {
        OAuthProfile profile = new OAuthProfile(
                SocialProvider.KAKAO,
                "kakao-user-100",
                null
        );
        when(providerClient.authorizationUri(
                eq(SocialProvider.KAKAO),
                anyString()
        )).thenAnswer(invocation -> URI.create(
                "https://kauth.kakao.com/oauth/authorize?state="
                        + invocation.getArgument(1, String.class)
        ));
        when(providerClient.fetchProfile(
                eq(SocialProvider.KAKAO),
                eq("authorization-code"),
                anyString()
        )).thenReturn(profile);

        MvcResult start = startOAuth("kakao");
        Cookie stateCookie = start.getResponse().getCookie(STATE_COOKIE);

        MvcResult callback = mockMvc.perform(
                        get("/api/auth/oauth/kakao/callback")
                                .param("code", "authorization-code")
                                .param("state", stateCookie.getValue())
                                .cookie(stateCookie)
                )
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(
                        "http://localhost:3000/oauth/onboarding"
                ))
                .andReturn();

        Cookie onboardingCookie = callback.getResponse()
                .getCookie(ONBOARDING_COOKIE);
        assertThat(onboardingCookie).isNotNull();
        assertThat(onboardingCookie.isHttpOnly()).isTrue();
        assertThat(onboardingCookie.getMaxAge()).isEqualTo(1200);
        assertThat(onboardingCookie.getPath())
                .isEqualTo("/api/auth/oauth/signup");
        assertThat(callback.getResponse().getCookie(STATE_COOKIE)
                .getMaxAge()).isZero();
        assertThat(userRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();
        assertThat(pendingRepository.count()).isEqualTo(1);

        MvcResult signup = mockMvc.perform(
                        post("/api/auth/oauth/signup")
                                .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                                .cookie(onboardingCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
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
                                          "nickname": "소셜사용자",
                                          "gender": "NOT_SPECIFIED",
                                          "notificationEmail": null
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.nickname")
                        .value("소셜사용자"))
                .andReturn();

        assertThat(signup.getResponse().getCookie(REFRESH_COOKIE))
                .isNotNull();
        assertThat(signup.getResponse().getCookie(ONBOARDING_COOKIE)
                .getMaxAge()).isZero();
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(socialAccountRepository.count()).isEqualTo(1);
        assertThat(termsAgreementRepository.count()).isEqualTo(3);
        assertThat(pendingRepository.findAll().getFirst()
                .getOnboardingTokenConsumedAt()).isNotNull();

        mockMvc.perform(
                        post("/api/auth/oauth/signup")
                                .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                                .cookie(onboardingCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
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
                                            }
                                          ],
                                          "nickname": "재사용시도",
                                          "gender": "NOT_SPECIFIED"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("SIGNUP_TOKEN_INVALID"));

        MvcResult secondStart = startOAuth("kakao");
        Cookie secondState = secondStart.getResponse().getCookie(STATE_COOKIE);

        MvcResult existingLogin = mockMvc.perform(
                        get("/api/auth/oauth/kakao/callback")
                                .param("code", "authorization-code")
                                .param("state", secondState.getValue())
                                .cookie(secondState)
                )
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(
                        "http://localhost:3000/oauth/success"
                ))
                .andReturn();

        assertThat(existingLogin.getResponse()
                .getCookie(REFRESH_COOKIE)).isNotNull();
        assertThat(existingLogin.getResponse().getContentAsString())
                .isEmpty();
        assertThat(existingLogin.getResponse().getRedirectedUrl())
                .doesNotContain("accessToken")
                .doesNotContain("token=");
    }

    @Test
    void 기존_일반계정과_같은_공급자_이메일은_자동연결하지_않는다()
            throws Exception {
        userRepository.saveAndFlush(
                User.local(
                        "same@example.com",
                        "일반사용자",
                        Gender.NOT_SPECIFIED
                )
        );
        when(providerClient.fetchProfile(
                eq(SocialProvider.NAVER),
                eq("authorization-code"),
                anyString()
        )).thenReturn(new OAuthProfile(
                SocialProvider.NAVER,
                "naver-user-200",
                "SAME@example.com"
        ));

        Cookie stateCookie = validStateCookie("naver");

        mockMvc.perform(
                        get("/api/auth/oauth/naver/callback")
                                .param("code", "authorization-code")
                                .param("state", stateCookie.getValue())
                                .cookie(stateCookie)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code")
                        .value("SOCIAL_EMAIL_CONFLICT"));

        assertThat(pendingRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();
    }

    @Test
    void 소셜_가입_POST도_허용된_Origin이_필요하다()
            throws Exception {
        mockMvc.perform(
                        post("/api/auth/oauth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("ORIGIN_NOT_ALLOWED"));
    }

    private MvcResult startOAuth(String provider) throws Exception {
        return mockMvc.perform(get("/api/auth/oauth/{provider}", provider))
                .andExpect(status().isFound())
                .andReturn();
    }

    private Cookie validStateCookie(String provider) throws Exception {
        when(providerClient.authorizationUri(
                eq(SocialProvider.fromPath(provider)),
                anyString()
        )).thenAnswer(invocation -> URI.create(
                "https://example.com/oauth?state="
                        + invocation.getArgument(1, String.class)
        ));
        return startOAuth(provider).getResponse().getCookie(STATE_COOKIE);
    }
}
