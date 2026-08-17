package org.likelionhsu.hackathon.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.place.domain.PlaceCategory;
import org.likelionhsu.hackathon.place.dto.PlaceSavedStateResponse;
import org.likelionhsu.hackathon.place.dto.SavedPlaceResponse;
import org.likelionhsu.hackathon.place.service.PlaceService;
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
        controllers = PlaceController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        SavedPlaceControllerTest.SecurityArgumentResolverConfig.class
})
class SavedPlaceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PlaceService placeService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savedListUsesV03PathAndPagination() throws Exception {
        authenticate(1L);

        SavedPlaceResponse item = new SavedPlaceResponse(
                "1001",
                "성수 카페",
                PlaceCategory.CAFE,
                "음식점 > 카페",
                "서울",
                "서울 도로명",
                new BigDecimal("37.5412"),
                new BigDecimal("127.0563"),
                "https://place.map.kakao.com/1001",
                true,
                Instant.parse("2026-08-18T00:00:00Z")
        );

        when(placeService.getSavedPlaces(
                eq(1L),
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

        mockMvc.perform(get("/api/places/saved")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].placeId")
                        .value("1001"))
                .andExpect(jsonPath("$.data.items[0].saved")
                        .value(true))
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1));
    }

    @Test
    void saveUsesPutAndReturnsSavedState() throws Exception {
        authenticate(1L);

        when(placeService.savePlace(1L, 1001L))
                .thenReturn(new PlaceSavedStateResponse(
                        "1001",
                        true
                ));

        mockMvc.perform(put("/api/places/1001/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placeId")
                        .value("1001"))
                .andExpect(jsonPath("$.data.saved")
                        .value(true));
    }

    @Test
    void unsaveReturns204() throws Exception {
        authenticate(1L);

        mockMvc.perform(delete("/api/places/1001/saved"))
                .andExpect(status().isNoContent());
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
