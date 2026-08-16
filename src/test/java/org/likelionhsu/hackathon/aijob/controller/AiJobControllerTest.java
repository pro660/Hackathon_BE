package org.likelionhsu.hackathon.aijob.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.aijob.domain.AiJobStatus;
import org.likelionhsu.hackathon.aijob.domain.AiJobType;
import org.likelionhsu.hackathon.aijob.dto.request.AiJobCreateRequest;
import org.likelionhsu.hackathon.aijob.dto.response.AiJobCreateResponse;
import org.likelionhsu.hackathon.aijob.service.AiJobService;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
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
        controllers = AiJobController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        AiJobControllerTest.SecurityArgumentResolverConfig.class
})
class AiJobControllerTest {

    private static final Long USER_ID = 1L;
    private static final String IDEMPOTENCY_KEY =
            "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiJobService aiJobService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void newPendingJobReturns202() throws Exception {
        authenticate(USER_ID);

        when(aiJobService.create(
                eq(USER_ID),
                eq(IDEMPOTENCY_KEY),
                any(AiJobCreateRequest.class)
        )).thenReturn(
                result(
                        AiJobStatus.PENDING,
                        true
                )
        );

        mockMvc.perform(
                        post("/api/ai-jobs")
                                .header(
                                        "Idempotency-Key",
                                        IDEMPOTENCY_KEY
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "type": "PURCHASE_UTILITY",
                                          "context": {
                                            "productId": "123"
                                          }
                                        }
                                        """)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success")
                        .value(true))
                .andExpect(jsonPath("$.data.jobId")
                        .value("9001"))
                .andExpect(jsonPath("$.data.type")
                        .value("PURCHASE_UTILITY"))
                .andExpect(jsonPath("$.data.status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.data.cached")
                        .doesNotExist());
    }

    @Test
    void completedReplayReturns200() throws Exception {
        authenticate(USER_ID);

        when(aiJobService.create(
                eq(USER_ID),
                eq(IDEMPOTENCY_KEY),
                any(AiJobCreateRequest.class)
        )).thenReturn(
                result(
                        AiJobStatus.SUCCEEDED,
                        false
                )
        );

        mockMvc.perform(
                        post("/api/ai-jobs")
                                .header(
                                        "Idempotency-Key",
                                        IDEMPOTENCY_KEY
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "type": "PURCHASE_UTILITY",
                                          "context": {
                                            "productId": "123"
                                          }
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId")
                        .value("9001"))
                .andExpect(jsonPath("$.data.status")
                        .value("SUCCEEDED"));
    }

    @Test
    void missingIdempotencyKeyReturns400() throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        post("/api/ai-jobs")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "type": "PURCHASE_UTILITY",
                                          "context": {
                                            "productId": "123"
                                          }
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(aiJobService);
    }

    @Test
    void blankProductIdReturns400() throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        post("/api/ai-jobs")
                                .header(
                                        "Idempotency-Key",
                                        IDEMPOTENCY_KEY
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "type": "PURCHASE_UTILITY",
                                          "context": {
                                            "productId": " "
                                          }
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(aiJobService);
    }

    @Test
    void idempotencyConflictReturns409() throws Exception {
        authenticate(USER_ID);

        when(aiJobService.create(
                eq(USER_ID),
                eq(IDEMPOTENCY_KEY),
                any(AiJobCreateRequest.class)
        )).thenThrow(
                new BusinessException(
                        ErrorCode.IDEMPOTENCY_KEY_CONFLICT
                )
        );

        mockMvc.perform(
                        post("/api/ai-jobs")
                                .header(
                                        "Idempotency-Key",
                                        IDEMPOTENCY_KEY
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "type": "PURCHASE_UTILITY",
                                          "context": {
                                            "productId": "124"
                                          }
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code")
                        .value(
                                "IDEMPOTENCY_KEY_CONFLICT"
                        ));
    }

    private AiJobService.CreationResult result(
            AiJobStatus status,
            boolean accepted
    ) {
        return new AiJobService.CreationResult(
                new AiJobCreateResponse(
                        "9001",
                        AiJobType.PURCHASE_UTILITY,
                        status,
                        Instant.parse(
                                "2026-08-17T00:00:00Z"
                        )
                ),
                accepted
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
