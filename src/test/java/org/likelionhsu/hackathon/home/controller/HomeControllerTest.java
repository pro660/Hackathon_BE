package org.likelionhsu.hackathon.home.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.home.dto.HomeResponse;
import org.likelionhsu.hackathon.home.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(
        controllers = HomeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        HomeControllerTest.SecurityArgumentResolverConfig.class
})
class HomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HomeService homeService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getHomeReturnsAggregateShape() throws Exception {
        authenticate(1L);

        when(homeService.getHome(1L))
                .thenReturn(new HomeResponse(
                        new HomeResponse.UserSummary("오늘뭐입지", true, 8L),
                        new HomeResponse.LatestStylePlan(
                                "601",
                                "데이트 룩",
                                "https://example.com/item.webp"
                        ),
                        List.of(new HomeResponse.RecommendedProduct(
                                "101",
                                "Aren Shopper",
                                new BigDecimal("82.00"),
                                "https://example.com/product.webp"
                        ))
                ));

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.nickname").value("오늘뭐입지"))
                .andExpect(jsonPath("$.data.user.preferenceCompleted").value(true))
                .andExpect(jsonPath("$.data.user.myItemCount").value(8))
                .andExpect(jsonPath("$.data.latestStylePlan.stylePlanId").value("601"))
                .andExpect(jsonPath("$.data.recommendedProducts[0].productId").value("101"))
                .andExpect(jsonPath("$.data.recommendedProducts[0].matchScore").value(82.0));
    }

    @Test
    void nullLatestStylePlanAndEmptyRecommendationsArePreserved() throws Exception {
        authenticate(1L);

        when(homeService.getHome(1L))
                .thenReturn(new HomeResponse(
                        new HomeResponse.UserSummary("새사용자", false, 0L),
                        null,
                        List.of()
                ));

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestStylePlan").value(nullValue()))
                .andExpect(jsonPath("$.data.recommendedProducts").isArray())
                .andExpect(jsonPath("$.data.recommendedProducts").isEmpty());
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
    static class SecurityArgumentResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
