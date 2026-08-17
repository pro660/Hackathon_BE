package org.likelionhsu.hackathon.aijob.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        AiJobStylePlanControllerTest
                .SecurityArgumentResolverConfig.class
})
class AiJobStylePlanControllerTest {

    private static final Long USER_ID = 1L;
    private static final String IDEMPOTENCY_KEY =
            "style-plan-controller-key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiJobService aiJobService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validStylePlanRequestReturns202()
            throws Exception {
        authenticate(USER_ID);

        when(aiJobService.create(
                eq(USER_ID),
                eq(IDEMPOTENCY_KEY),
                any(AiJobCreateRequest.class)
        )).thenReturn(
                new AiJobService.CreationResult(
                        new AiJobCreateResponse(
                                "9201",
                                AiJobType.STYLE_PLAN,
                                AiJobStatus.PENDING,
                                Instant.parse(
                                        "2026-08-18T00:00:00Z"
                                )
                        ),
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
                                          "type": "STYLE_PLAN",
                                          "context": {
                                            "occasion": "DATE",
                                            "styleTags": [
                                              "NEAT",
                                              "GLAMOROUS"
                                            ],
                                            "weatherCondition": null,
                                            "prioritizeOwnedItems": true,
                                            "language": "ko"
                                          }
                                        }
                                        """)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success")
                        .value(true))
                .andExpect(jsonPath("$.data.jobId")
                        .value("9201"))
                .andExpect(jsonPath("$.data.type")
                        .value("STYLE_PLAN"))
                .andExpect(jsonPath("$.data.status")
                        .value("PENDING"));
    }

    @Test
    void runningJobPolicyReturns409()
            throws Exception {
        authenticate(USER_ID);

        when(aiJobService.create(
                eq(USER_ID),
                eq(IDEMPOTENCY_KEY),
                any(AiJobCreateRequest.class)
        )).thenThrow(
                new BusinessException(
                        ErrorCode.AI_JOB_ALREADY_RUNNING
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
                                .content(validRequest())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code")
                        .value("AI_JOB_ALREADY_RUNNING"));
    }

    @Test
    void dailyLimitPolicyReturns429()
            throws Exception {
        authenticate(USER_ID);

        when(aiJobService.create(
                eq(USER_ID),
                eq(IDEMPOTENCY_KEY),
                any(AiJobCreateRequest.class)
        )).thenThrow(
                new BusinessException(
                        ErrorCode.AI_DAILY_LIMIT_EXCEEDED
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
                                .content(validRequest())
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code")
                        .value("AI_DAILY_LIMIT_EXCEEDED"));
    }

    private String validRequest() {
        return """
                {
                  "type": "STYLE_PLAN",
                  "context": {
                    "occasion": "DATE",
                    "styleTags": ["NEAT"],
                    "weatherCondition": null,
                    "prioritizeOwnedItems": true,
                    "language": "ko"
                  }
                }
                """;
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
