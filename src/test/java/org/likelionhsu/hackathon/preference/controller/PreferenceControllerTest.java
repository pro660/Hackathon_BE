package org.likelionhsu.hackathon.preference.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.preference.dto.request.PreferenceRequest;
import org.likelionhsu.hackathon.preference.dto.response.PreferenceResponse;
import org.likelionhsu.hackathon.preference.service.PreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(
        controllers = PreferenceController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        PreferenceControllerTest.SecurityArgumentResolverConfig.class
})
class PreferenceControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreferenceService preferenceService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void existingPreferenceReturns200() throws Exception {
        authenticate(USER_ID);

        PreferenceResponse response =
                new PreferenceResponse(
                        List.of(
                                "BEIGE",
                                "BLACK",
                                "WHITE"
                        ),
                        List.of(
                                "BAG",
                                "CLOTHING"
                        ),
                        List.of(
                                "CASUAL",
                                "NEAT"
                        )
                );

        when(
                preferenceService.getPreference(
                        USER_ID
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/preferences")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.data.preferredColors[0]"
                        ).value("BEIGE")
                )
                .andExpect(
                        jsonPath(
                                "$.data.preferredColors[1]"
                        ).value("BLACK")
                )
                .andExpect(
                        jsonPath(
                                "$.data.preferredColors[2]"
                        ).value("WHITE")
                )
                .andExpect(
                        jsonPath(
                                "$.data.preferredCategories[0]"
                        ).value("BAG")
                )
                .andExpect(
                        jsonPath(
                                "$.data.preferredStyleTags[0]"
                        ).value("CASUAL")
                )
                .andExpect(
                        jsonPath("$.data.id")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.userId")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.summary")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.confidence")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.analysisVersion")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.aiJobId")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.data.version")
                                .doesNotExist()
                );

        verify(preferenceService)
                .getPreference(USER_ID);
    }

    @Test
    void missingPreferenceReturns200WithNullData()
            throws Exception {

        authenticate(USER_ID);

        when(
                preferenceService.getPreference(
                        USER_ID
                )
        ).thenReturn(null);

        mockMvc.perform(
                        get("/api/preferences")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data")
                                .value(nullValue())
                );
    }

    @Test
    void firstPreferencePersistenceReturns200()
            throws Exception {

        authenticate(USER_ID);

        PreferenceResponse response =
                preferenceResponse();

        when(
                preferenceService.updatePreference(
                        eq(USER_ID),
                        any(PreferenceRequest.class)
                )
        ).thenReturn(response);

        performValidPut()
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.data.preferredColors[0]"
                        ).value("BLACK")
                )
                .andExpect(
                        jsonPath(
                                "$.data.preferredCategories[0]"
                        ).value("BAG")
                )
                .andExpect(
                        jsonPath(
                                "$.data.preferredStyleTags[0]"
                        ).value("CASUAL")
                );
    }

    @Test
    void existingPreferenceReplacementReturns200()
            throws Exception {

        authenticate(USER_ID);

        when(
                preferenceService.updatePreference(
                        eq(USER_ID),
                        any(PreferenceRequest.class)
                )
        ).thenReturn(
                preferenceResponse()
        );

        performValidPut()
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void jakartaValidationFailureReturns400()
            throws Exception {

        authenticate(USER_ID);

        mockMvc.perform(
                        put("/api/preferences")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "preferredColors": [],
                                          "preferredCategories": ["BAG"],
                                          "preferredStyleTags": ["CASUAL"]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].field"
                        ).value("preferredColors")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].reason"
                        ).value(
                                "1개 이상 3개 이하로 선택해 주세요."
                        )
                );

        verifyNoInteractions(
                preferenceService
        );
    }

    @Test
    void customValidationFailureReturns400()
            throws Exception {

        authenticate(USER_ID);

        when(
                preferenceService.updatePreference(
                        eq(USER_ID),
                        any(PreferenceRequest.class)
                )
        ).thenThrow(
                new RequestValidationException(
                        "preferredColors",
                        "허용되지 않은 값이 포함되어 있습니다."
                )
        );

        mockMvc.perform(
                        put("/api/preferences")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "preferredColors": ["black"],
                                          "preferredCategories": ["BAG"],
                                          "preferredStyleTags": ["CASUAL"]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].field"
                        ).value("preferredColors")
                )
                .andExpect(
                        jsonPath(
                                "$.error.fields[0].reason"
                        ).value(
                                "허용되지 않은 값이 포함되어 있습니다."
                        )
                );
    }

    @Test
    void malformedJsonReturns400()
            throws Exception {

        authenticate(USER_ID);

        mockMvc.perform(
                        put("/api/preferences")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "preferredColors": ["BLACK"],
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value(
                                        "REQUEST_BODY_INVALID"
                                )
                );

        verifyNoInteractions(
                preferenceService
        );
    }

    @Test
    void missingJwtUserReturns401()
            throws Exception {

        authenticate(USER_ID);

        when(
                preferenceService.getPreference(
                        USER_ID
                )
        ).thenThrow(
                new BusinessException(
                        ErrorCode.ACCESS_TOKEN_INVALID
                )
        );

        mockMvc.perform(
                        get("/api/preferences")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value(
                                        "ACCESS_TOKEN_INVALID"
                                )
                );
    }

    @Test
    void inactiveUserReturns403()
            throws Exception {

        authenticate(USER_ID);

        when(
                preferenceService.getPreference(
                        USER_ID
                )
        ).thenThrow(
                new BusinessException(
                        ErrorCode.ACCOUNT_NOT_ACTIVE
                )
        );

        mockMvc.perform(
                        get("/api/preferences")
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value(
                                        "ACCOUNT_NOT_ACTIVE"
                                )
                );
    }

    private org.springframework.test.web.servlet
            .ResultActions performValidPut()
            throws Exception {

        return mockMvc.perform(
                put("/api/preferences")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                """
                                {
                                  "preferredColors": ["BLACK"],
                                  "preferredCategories": ["BAG"],
                                  "preferredStyleTags": ["CASUAL"]
                                }
                                """
                        )
        );
    }

    private PreferenceResponse preferenceResponse() {
        return new PreferenceResponse(
                List.of("BLACK"),
                List.of("BAG"),
                List.of("CASUAL")
        );
    }

    private void authenticate(
            Long userId
    ) {
        Jwt jwt =
                Jwt.withTokenValue(
                                "test-token"
                        )
                        .header(
                                "alg",
                                "HS256"
                        )
                        .subject(
                                String.valueOf(
                                        userId
                                )
                        )
                        .build();

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                jwt
                        )
                );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityArgumentResolverConfig
            implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(
                List<HandlerMethodArgumentResolver>
                        resolvers
        ) {
            resolvers.add(
                    new AuthenticationPrincipalArgumentResolver()
            );
        }
    }
}