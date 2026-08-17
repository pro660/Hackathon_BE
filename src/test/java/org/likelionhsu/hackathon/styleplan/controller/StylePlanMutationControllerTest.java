package org.likelionhsu.hackathon.styleplan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanUpdateRequest;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanDetailResponse;
import org.likelionhsu.hackathon.styleplan.service.StylePlanService;
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
        controllers = StylePlanController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        StylePlanMutationControllerTest
                .SecurityArgumentResolverConfig.class
})
class StylePlanMutationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StylePlanService stylePlanService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void patchReturnsUpdatedDetail()
            throws Exception {
        authenticate(1L);

        Instant now = Instant.parse(
                "2026-08-18T00:00:00Z"
        );

        when(stylePlanService.updateStylePlan(
                eq(1L),
                eq(601L),
                any(StylePlanUpdateRequest.class)
        )).thenReturn(
                new StylePlanDetailResponse(
                        "601",
                        "주말 데이트 룩",
                        StylePlanOccasion.DATE,
                        null,
                        null,
                        "설명",
                        StylePlanGenerationType.AI,
                        StylePlanStatus.COMPLETED,
                        List.of(),
                        List.of(),
                        List.of(),
                        2L,
                        now,
                        now
                )
        );

        mockMvc.perform(
                        patch("/api/style-plans/601")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "title":"주말 데이트 룩",
                                          "plannedAt":null,
                                          "status":"COMPLETED",
                                          "version":1
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.title"
                ).value("주말 데이트 룩"))
                .andExpect(jsonPath(
                        "$.data.plannedAt"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$.data.status"
                ).value("COMPLETED"))
                .andExpect(jsonPath(
                        "$.data.version"
                ).value(2));
    }

    @Test
    void patchRequiresVersion()
            throws Exception {
        authenticate(1L);

        mockMvc.perform(
                        patch("/api/style-plans/601")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "title":"수정"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.error.code"
                ).value("VALIDATION_ERROR"));
    }

    @Test
    void stalePatchReturns409()
            throws Exception {
        authenticate(1L);

        when(stylePlanService.updateStylePlan(
                eq(1L),
                eq(601L),
                any(StylePlanUpdateRequest.class)
        )).thenThrow(
                new BusinessException(
                        ErrorCode.RESOURCE_VERSION_CONFLICT
                )
        );

        mockMvc.perform(
                        patch("/api/style-plans/601")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "title":"수정",
                                          "version":1
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath(
                        "$.error.code"
                ).value(
                        "RESOURCE_VERSION_CONFLICT"
                ));
    }

    @Test
    void deleteReturns204()
            throws Exception {
        authenticate(1L);

        doNothing().when(stylePlanService)
                .deleteStylePlan(1L, 601L);

        mockMvc.perform(
                        delete("/api/style-plans/601")
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteOtherUsersOrMissingPlanReturns404()
            throws Exception {
        authenticate(1L);

        org.mockito.Mockito.doThrow(
                new BusinessException(
                        ErrorCode.STYLE_PLAN_NOT_FOUND
                )
        ).when(stylePlanService)
                .deleteStylePlan(1L, 999L);

        mockMvc.perform(
                        delete("/api/style-plans/999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(
                        "$.error.code"
                ).value("STYLE_PLAN_NOT_FOUND"));
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
