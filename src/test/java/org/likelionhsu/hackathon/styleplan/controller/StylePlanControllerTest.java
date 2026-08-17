package org.likelionhsu.hackathon.styleplan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.styleplan.dto.request.StylePlanCreateRequest;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanCreateResponse;
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
        StylePlanControllerTest
                .SecurityArgumentResolverConfig.class
})
class StylePlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StylePlanService stylePlanService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createStylePlanReturns201()
            throws Exception {
        authenticate(1L);

        when(stylePlanService.create(
                eq(1L),
                any(StylePlanCreateRequest.class)
        )).thenReturn(
                new StylePlanCreateResponse("601")
        );

        mockMvc.perform(
                        post("/api/style-plans")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "aiJobId":"9001",
                                          "title":"데이트 룩",
                                          "occasion":"DATE",
                                          "plannedAt":"2026-08-20T10:00:00Z",
                                          "weatherCondition":null,
                                          "description":"깔끔한 보유 아이템 중심",
                                          "status":"CONFIRMED",
                                          "ownedItems":[
                                            {
                                              "myItemId":"501",
                                              "role":"BAG",
                                              "sortOrder":0
                                            }
                                          ],
                                          "recommendedProducts":[
                                            {
                                              "productId":"101",
                                              "rank":1,
                                              "reason":"잘 어울려요."
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success")
                        .value(true))
                .andExpect(
                        jsonPath("$.data.stylePlanId")
                                .value("601")
                );
    }

    @Test
    void moreThanThreeProductsReturns400()
            throws Exception {
        authenticate(1L);

        mockMvc.perform(
                        post("/api/style-plans")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "title":"데이트 룩",
                                          "occasion":"DATE",
                                          "status":"CONFIRMED",
                                          "ownedItems":[],
                                          "recommendedProducts":[
                                            {"productId":"1","rank":1},
                                            {"productId":"2","rank":2},
                                            {"productId":"3","rank":3},
                                            {"productId":"4","rank":4}
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("VALIDATION_ERROR"));
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
