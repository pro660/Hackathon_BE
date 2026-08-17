package org.likelionhsu.hackathon.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.dto.PlaceResponse;
import org.likelionhsu.hackathon.place.dto.PlaceSearchResponse;
import org.likelionhsu.hackathon.place.service.PlaceService;
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
        controllers = PlaceController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        PlaceControllerTest.SecurityArgumentResolverConfig.class
})
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchReturnsV03Shape() throws Exception {
        authenticate(1L);

        when(placeService.search(
                eq(1L),
                eq("성수"),
                eq(PlaceCategory.CAFE),
                any(BigDecimal.class),
                any(BigDecimal.class),
                eq(3000)
        )).thenReturn(new PlaceSearchResponse(List.of(
                new PlaceResponse(
                        "1001",
                        "성수 카페",
                        PlaceCategory.CAFE,
                        "음식점 > 카페",
                        "서울 성동구 성수동",
                        "서울 성동구 성수이로",
                        new BigDecimal("37.5412"),
                        new BigDecimal("127.0563"),
                        "https://place.map.kakao.com/100",
                        true
                )
        )));

        mockMvc.perform(
                        get("/api/places")
                                .param("query", "성수")
                                .param("category", "CAFE")
                                .param("latitude", "37.5445")
                                .param("longitude", "127.0560")
                                .param("radius", "3000")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].placeId").value("1001"))
                .andExpect(jsonPath("$.data.items[0].category").value("CAFE"))
                .andExpect(jsonPath("$.data.items[0].saved").value(true));
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
