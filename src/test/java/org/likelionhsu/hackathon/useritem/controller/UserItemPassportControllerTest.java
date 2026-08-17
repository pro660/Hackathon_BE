package org.likelionhsu.hackathon.useritem.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemPassportProductInfoResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemPassportPurchaseInfoResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemPassportResponse;
import org.likelionhsu.hackathon.useritem.service.UserItemService;
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
        controllers = UserItemController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        UserItemPassportControllerTest.SecurityArgumentResolverConfig.class
})
class UserItemPassportControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserItemService userItemService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passportEndpointReturnsProductAndPurchaseInfo() throws Exception {
        authenticate(USER_ID);

        when(userItemService.getMyItemPassport(USER_ID, 10L))
                .thenReturn(
                        new UserItemPassportResponse(
                                "10",
                                new UserItemPassportProductInfoResponse(
                                        "20",
                                        "MCM",
                                        "내 가방",
                                        ItemCategory.BAG,
                                        ColorGroup.BLACK,
                                        MaterialGroup.LEATHER,
                                        "https://example.com/item.webp",
                                        "SKU-20",
                                        "https://example.com/products/20"
                                ),
                                new UserItemPassportPurchaseInfoResponse(
                                        "ORDER-2026-001",
                                        LocalDate.of(2026, 6, 1),
                                        1_500_000L,
                                        "MCM 청담점"
                                )
                        )
                );

        mockMvc.perform(get("/api/my-items/10/passport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.myItemId").value("10"))
                .andExpect(jsonPath("$.data.productInfo.linkedProductId")
                        .value("20"))
                .andExpect(jsonPath("$.data.productInfo.name")
                        .value("내 가방"))
                .andExpect(jsonPath("$.data.productInfo.sku")
                        .value("SKU-20"))
                .andExpect(jsonPath("$.data.purchaseInfo.purchaseOrderNumber")
                        .value("ORDER-2026-001"))
                .andExpect(jsonPath("$.data.purchaseInfo.purchaseDate")
                        .value("2026-06-01"))
                .andExpect(jsonPath("$.data.purchaseInfo.purchasePrice")
                        .value(1500000))
                .andExpect(jsonPath("$.data.purchaseInfo.purchasePlace")
                        .value("MCM 청담점"));
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
