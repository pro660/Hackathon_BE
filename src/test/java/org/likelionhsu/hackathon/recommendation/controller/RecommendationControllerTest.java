package org.likelionhsu.hackathon.recommendation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.product.dto.response.ProductTagsResponse;
import org.likelionhsu.hackathon.recommendation.dto.request.RecommendationRequest;
import org.likelionhsu.hackathon.recommendation.dto.response.RecommendationProductResponse;
import org.likelionhsu.hackathon.recommendation.dto.response.RecommendationResponse;
import org.likelionhsu.hackathon.recommendation.dto.response.RecommendationScoreBreakdownResponse;
import org.likelionhsu.hackathon.recommendation.entity.RecommendationGenerationType;
import org.likelionhsu.hackathon.recommendation.service.RecommendationService;
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
        controllers = RecommendationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        RecommendationControllerTest.SecurityArgumentResolverConfig.class
})
class RecommendationControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReturns201WithRecommendationResponse() throws Exception {
        authenticate(USER_ID);
        when(recommendationService.createRecommendation(
                eq(USER_ID),
                any(RecommendationRequest.class)
        )).thenReturn(response());

        mockMvc.perform(
                        post("/api/recommendations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "occasion": "DATE",
                                          "season": "AUTUMN",
                                          "preferredFeatures": ["COMPACT", "MULTIWAY"],
                                          "category": "BAG",
                                          "limit": 3
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendationId").value("31"))
                .andExpect(jsonPath("$.data.generationType").value("RULE_BASED"))
                .andExpect(jsonPath("$.data.scorePolicyVersion")
                        .value("product-recommendation-v1"))
                .andExpect(jsonPath("$.data.products[0].score").value(90.0))
                .andExpect(jsonPath("$.data.products[0].scoreBreakdown.style")
                        .value(30.0))
                .andExpect(jsonPath("$.data.products[0].favorited").value(true));
    }

    @Test
    void getReturns200WithSameResponseShape() throws Exception {
        authenticate(USER_ID);
        when(recommendationService.getRecommendation(USER_ID, 31L))
                .thenReturn(response());

        mockMvc.perform(get("/api/recommendations/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendationId").value("31"))
                .andExpect(jsonPath("$.data.products[0].productId").value("15"));
    }

    @Test
    void preferenceRequiredReturns409() throws Exception {
        authenticate(USER_ID);
        when(recommendationService.createRecommendation(
                eq(USER_ID),
                any(RecommendationRequest.class)
        )).thenThrow(new BusinessException(ErrorCode.PREFERENCE_REQUIRED));

        mockMvc.perform(
                        post("/api/recommendations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "occasion": "DATE",
                                          "season": "AUTUMN",
                                          "preferredFeatures": ["COMPACT"]
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PREFERENCE_REQUIRED"));
    }

    @Test
    void recommendationNotFoundReturns404() throws Exception {
        authenticate(USER_ID);
        when(recommendationService.getRecommendation(USER_ID, 999L))
                .thenThrow(new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        mockMvc.perform(get("/api/recommendations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code")
                        .value("RECOMMENDATION_NOT_FOUND"));
    }

    @Test
    void invalidFeatureCountReturns400BeforeService() throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        post("/api/recommendations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "occasion": "DATE",
                                          "season": "AUTUMN",
                                          "preferredFeatures": []
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field")
                        .value("preferredFeatures"));

        verifyNoInteractions(recommendationService);
    }

    private RecommendationResponse response() {
        return new RecommendationResponse(
                "31",
                RecommendationGenerationType.RULE_BASED,
                "product-recommendation-v1",
                "데이트와 가을 시즌을 기준으로 추천했어요.",
                List.of(
                        new RecommendationProductResponse(
                                "15",
                                "Aren Shopper",
                                ItemCategory.BAG,
                                1_250_000L,
                                ColorGroup.BLACK,
                                "https://example.com/product.webp",
                                new ProductTagsResponse(
                                        List.of("CASUAL"),
                                        List.of("AUTUMN"),
                                        List.of("DATE"),
                                        List.of("COMPACT")
                                ),
                                new BigDecimal("90.00"),
                                new RecommendationScoreBreakdownResponse(
                                        new BigDecimal("30.00"),
                                        new BigDecimal("25.00"),
                                        new BigDecimal("25.00"),
                                        new BigDecimal("10.00")
                                ),
                                "선호 스타일과 데이트 상황에 잘 맞는 제품입니다.",
                                true
                        )
                ),
                Instant.parse("2026-08-16T00:00:00Z")
        );
    }

    private void authenticate(Long userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(String.valueOf(userId))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
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
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
