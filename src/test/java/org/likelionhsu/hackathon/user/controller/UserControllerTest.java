package org.likelionhsu.hackathon.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.user.dto.request.UserProfileUpdateRequest;
import org.likelionhsu.hackathon.user.dto.response.UserProfileResponse;
import org.likelionhsu.hackathon.user.service.UserService;
import org.likelionhsu.hackathon.user.service.AccountDeletionService;
import org.likelionhsu.hackathon.auth.support.ReauthenticationCookieService;
import org.likelionhsu.hackathon.auth.support.RefreshCookieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        UserControllerTest.SecurityArgumentResolverConfig.class
})
class UserControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AccountDeletionService accountDeletionService;

    @MockitoBean
    private ReauthenticationCookieService
            reauthenticationCookieService;

    @MockitoBean
    private RefreshCookieService refreshCookieService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myProfileReturns200() throws Exception {
        authenticate(USER_ID);

        when(userService.getMyProfile(USER_ID))
                .thenReturn(profileResponse());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("1"))
                .andExpect(jsonPath("$.data.nickname").value("오늘뭐입지"))
                .andExpect(jsonPath("$.data.gender").value("NOT_SPECIFIED"))
                .andExpect(jsonPath(
                        "$.data.authenticationMethods[0]"
                ).value("LOCAL"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.version").doesNotExist());

        verify(userService).getMyProfile(USER_ID);
    }

    @Test
    void partialProfileUpdateReturns200() throws Exception {
        authenticate(USER_ID);

        when(userService.updateMyProfile(
                eq(USER_ID),
                any(UserProfileUpdateRequest.class)
        )).thenReturn(profileResponse());

        mockMvc.perform(
                        patch("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "nickname": "오늘뭐입지"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("오늘뭐입지"));
    }

    @Test
    void emptyPatchReturns400() throws Exception {
        authenticate(USER_ID);

        when(userService.updateMyProfile(
                eq(USER_ID),
                any(UserProfileUpdateRequest.class)
        )).thenThrow(
                new RequestValidationException(
                        "request",
                        "수정할 필드를 하나 이상 입력해 주세요."
                )
        );

        mockMvc.perform(
                        patch("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("request"));
    }

    @Test
    void invalidGenderReturns400() throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        patch("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "gender": "UNKNOWN"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error.code")
                                .value("REQUEST_BODY_INVALID")
                );

        verifyNoInteractions(userService);
    }

    @Test
    void missingJwtUserReturns401() throws Exception {
        authenticate(USER_ID);

        when(userService.getMyProfile(USER_ID))
                .thenThrow(
                        new BusinessException(
                                ErrorCode.ACCESS_TOKEN_INVALID
                        )
                );

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.error.code")
                                .value("ACCESS_TOKEN_INVALID")
                );
    }

    @Test
    void inactiveUserReturns403() throws Exception {
        authenticate(USER_ID);

        when(userService.getMyProfile(USER_ID))
                .thenThrow(
                        new BusinessException(
                                ErrorCode.ACCOUNT_NOT_ACTIVE
                        )
                );

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.error.code")
                                .value("ACCOUNT_NOT_ACTIVE")
                );
    }

    @Test
    void verifiedAccountDeletionReturns204AndClearsCookies()
            throws Exception {
        authenticate(USER_ID);
        when(reauthenticationCookieService.read(any()))
                .thenReturn("raw-token");
        when(reauthenticationCookieService.clear())
                .thenReturn(ResponseCookie.from(
                        "account_reauth_token",
                        ""
                ).build());
        when(refreshCookieService.clear())
                .thenReturn(ResponseCookie.from(
                        "refresh_token",
                        ""
                ).build());

        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());

        verify(accountDeletionService)
                .deleteAccount(USER_ID, "raw-token");
    }

    private UserProfileResponse profileResponse() {
        return new UserProfileResponse(
                "1",
                "오늘뭐입지",
                Gender.NOT_SPECIFIED,
                List.of("LOCAL")
        );
    }

    private void authenticate(Long userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(String.valueOf(userId))
                .build();

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(jwt)
                );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityArgumentResolverConfig
            implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(
                List<HandlerMethodArgumentResolver> resolvers
        ) {
            resolvers.add(
                    new AuthenticationPrincipalArgumentResolver()
            );
        }
    }
}
