package org.likelionhsu.hackathon.styleplan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.styleplan.domain.StyleItemRole;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanGenerationType;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanOccasion;
import org.likelionhsu.hackathon.styleplan.domain.StylePlanStatus;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanDetailResponse;
import org.likelionhsu.hackathon.styleplan.dto.response.StylePlanListItemResponse;
import org.likelionhsu.hackathon.styleplan.service.StylePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
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
        StylePlanQueryControllerTest.SecurityArgumentResolverConfig.class
})
class StylePlanQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StylePlanService stylePlanService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listReturnsPagedItems() throws Exception {
        authenticate(1L);

        StylePlanListItemResponse item = new StylePlanListItemResponse(
                "601",
                "데이트 룩",
                StylePlanOccasion.DATE,
                null,
                StylePlanStatus.CONFIRMED,
                "https://example.com/item.webp",
                2,
                1,
                Instant.parse("2026-08-18T00:00:00Z")
        );

        when(stylePlanService.getStylePlans(
                eq(1L),
                eq(StylePlanStatus.CONFIRMED),
                any(Pageable.class)
        )).thenReturn(new PageResponse<>(
                List.of(item),
                0,
                20,
                1,
                1,
                false,
                false
        ));

        mockMvc.perform(
                        get("/api/style-plans")
                                .param("status", "CONFIRMED")
                                .param("page", "0")
                                .param("size", "20")
                                .param("sort", "createdAt,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].stylePlanId")
                        .value("601"))
                .andExpect(jsonPath("$.data.items[0].ownedItemCount")
                        .value(2))
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1));
    }

    @Test
    void detailReturnsComposition() throws Exception {
        authenticate(1L);
        Instant now = Instant.parse("2026-08-18T00:00:00Z");

        when(stylePlanService.getStylePlan(1L, 601L)).thenReturn(
                new StylePlanDetailResponse(
                        "601",
                        "데이트 룩",
                        StylePlanOccasion.DATE,
                        null,
                        null,
                        "설명",
                        StylePlanGenerationType.AI,
                        StylePlanStatus.CONFIRMED,
                        List.of(new StylePlanDetailResponse.OwnedItem(
                                "501",
                                "브라운 데일리백",
                                "https://example.com/item.webp",
                                StyleItemRole.BAG,
                                0
                        )),
                        List.of(new StylePlanDetailResponse.RecommendedProduct(
                                "101",
                                "Aren Shopper",
                                "https://example.com/product.webp",
                                1,
                                "잘 어울려요."
                        )),
                        List.of(),
                        0L,
                        now,
                        now
                )
        );

        mockMvc.perform(get("/api/style-plans/601"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stylePlanId").value("601"))
                .andExpect(jsonPath("$.data.ownedItems[0].myItemId")
                        .value("501"))
                .andExpect(jsonPath("$.data.recommendedProducts[0].productId")
                        .value("101"))
                .andExpect(jsonPath("$.data.places").isArray())
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    void missingOrOtherUsersPlanReturns404() throws Exception {
        authenticate(1L);

        when(stylePlanService.getStylePlan(1L, 999L)).thenThrow(
                new BusinessException(ErrorCode.STYLE_PLAN_NOT_FOUND)
        );

        mockMvc.perform(get("/api/style-plans/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code")
                        .value("STYLE_PLAN_NOT_FOUND"));
    }

    @Test
    void invalidSortReturns400() throws Exception {
        authenticate(1L);

        mockMvc.perform(
                        get("/api/style-plans")
                                .param("sort", "unknown,desc")
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

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt)
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityArgumentResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(
                List<HandlerMethodArgumentResolver> resolvers
        ) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
