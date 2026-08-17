package org.likelionhsu.hackathon.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.dto.PlaceRecommendationResponse;
import org.likelionhsu.hackathon.place.service.PlaceRecommendationService;
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
        controllers = StylePlanPlaceRecommendationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        StylePlanPlaceRecommendationControllerTest.SecurityArgumentResolverConfig.class
})
class StylePlanPlaceRecommendationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PlaceRecommendationService recommendationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recommendReturnsRuleBasedRanking() throws Exception {
        authenticate(1L);

        when(recommendationService.recommend(
                eq(1L),
                eq(601L),
                any()
        )).thenReturn(new PlaceRecommendationResponse(
                "601",
                "place-ranking-v1",
                List.of(new PlaceRecommendationResponse.RecommendedPlace(
                        1,
                        92.0,
                        new PlaceRecommendationResponse.ScoreBreakdown(
                                60.0,
                                32.0
                        ),
                        "OCCASION_CATEGORY_AND_DISTANCE_MATCH",
                        new PlaceRecommendationResponse.Place(
                                "1001",
                                "성수 카페",
                                PlaceCategory.CAFE,
                                "음식점 > 카페",
                                "서울 성동구",
                                new BigDecimal("37.5412"),
                                new BigDecimal("127.0563"),
                                "https://place.map.kakao.com/1001",
                                false
                        )
                ))
        ));

        mockMvc.perform(post(
                        "/api/style-plans/601/place-recommendations"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 37.5445,
                                  "longitude": 127.0560,
                                  "radius": 3000,
                                  "category": null,
                                  "query": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stylePlanId")
                        .value("601"))
                .andExpect(jsonPath("$.data.rankingPolicyVersion")
                        .value("place-ranking-v1"))
                .andExpect(jsonPath("$.data.places[0].score")
                        .value(92.0))
                .andExpect(jsonPath(
                        "$.data.places[0].scoreBreakdown.categorySuitability"
                ).value(60.0));
    }

    @Test
    void missingLatitudeReturns400() throws Exception {
        authenticate(1L);

        mockMvc.perform(post(
                        "/api/style-plans/601/place-recommendations"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "longitude": 127.0560
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("VALIDATION_ERROR"));
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
            resolvers.add(
                    new AuthenticationPrincipalArgumentResolver()
            );
        }
    }
}
