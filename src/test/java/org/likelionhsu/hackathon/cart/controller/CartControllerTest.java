package org.likelionhsu.hackathon.cart.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.cart.dto.response.CartItemResponse;
import org.likelionhsu.hackathon.cart.service.CartService;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(
        controllers = CartController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        CartControllerTest.SecurityArgumentResolverConfig.class
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cartItemCanBeAdded() throws Exception {
        authenticate(1L);

        mockMvc.perform(
                        put("/api/products/10/cart")
                )
                .andExpect(
                        status().isNoContent()
                )
                .andExpect(
                        content().string("")
                );

        verify(cartService)
                .addCartItem(
                        1L,
                        10L
                );
    }

    @Test
    void cartItemCanBeRemoved() throws Exception {
        authenticate(1L);

        mockMvc.perform(
                        delete("/api/products/10/cart")
                )
                .andExpect(
                        status().isNoContent()
                )
                .andExpect(
                        content().string("")
                );

        verify(cartService)
                .removeCartItem(
                        1L,
                        10L
                );
    }

    @Test
    void missingProductReturns404WhenAddingCartItem()
            throws Exception {
        authenticate(1L);

        doThrow(
                new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND
                )
        )
                .when(cartService)
                .addCartItem(
                        1L,
                        999L
                );

        mockMvc.perform(
                        put("/api/products/999/cart")
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("PRODUCT_NOT_FOUND")
                );
    }

    @Test
    void zeroProductIdReturnsValidationError()
            throws Exception {
        authenticate(1L);

        mockMvc.perform(
                        put("/api/products/0/cart")
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("productId")
                );
    }

    @Test
    void cartItemsReturnDefaultPage() throws Exception {
        authenticate(1L);

        PageResponse<CartItemResponse> response =
                new PageResponse<>(
                        List.of(
                                new CartItemResponse(
                                        "30",
                                        "10",
                                        ProductBrand.MCM,
                                        "MCM Black Bag",
                                        1_500_000L,
                                        "https://example.com/product.webp",
                                        "https://kr.mcmworldwide.com/product",
                                        Instant.parse(
                                                "2026-08-18T00:00:00Z"
                                        )
                                )
                        ),
                        0,
                        20,
                        1,
                        1,
                        false,
                        false
                );

        when(
                cartService.getCartItems(
                        eq(1L),
                        any(Pageable.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/cart-items")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data.items[0].cartItemId")
                                .value("30")
                )
                .andExpect(
                        jsonPath("$.data.items[0].productId")
                                .value("10")
                )
                .andExpect(
                        jsonPath("$.data.items[0].brand")
                                .value("MCM")
                )
                .andExpect(
                        jsonPath("$.data.items[0].name")
                                .value("MCM Black Bag")
                )
                .andExpect(
                        jsonPath("$.data.items[0].price")
                                .value(1_500_000)
                )
                .andExpect(
                        jsonPath("$.data.items[0].primaryImageUrl")
                                .value(
                                        "https://example.com/product.webp"
                                )
                )
                .andExpect(
                        jsonPath("$.data.items[0].productUrl")
                                .value(
                                        "https://kr.mcmworldwide.com/product"
                                )
                )
                .andExpect(
                        jsonPath("$.data.items[0].addedAt")
                                .value(
                                        "2026-08-18T00:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.data.page")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.data.size")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.data.totalElements")
                                .value(1)
                );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(cartService)
                .getCartItems(
                        eq(1L),
                        pageableCaptor.capture()
                );

        Pageable pageable =
                pageableCaptor.getValue();

        assertThat(pageable.getPageNumber())
                .isZero();

        assertThat(pageable.getPageSize())
                .isEqualTo(20);

        assertThat(
                pageable
                        .getSort()
                        .getOrderFor("createdAt")
        )
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void cartItemsAcceptCustomPageAndSort()
            throws Exception {
        authenticate(1L);

        PageResponse<CartItemResponse> response =
                new PageResponse<>(
                        List.of(),
                        2,
                        10,
                        0,
                        0,
                        false,
                        true
                );

        when(
                cartService.getCartItems(
                        eq(1L),
                        any(Pageable.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/cart-items")
                                .param("page", "2")
                                .param("size", "10")
                                .param(
                                        "sort",
                                        "createdAt,asc"
                                )
                )
                .andExpect(
                        status().isOk()
                );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(cartService)
                .getCartItems(
                        eq(1L),
                        pageableCaptor.capture()
                );

        Pageable pageable =
                pageableCaptor.getValue();

        assertThat(pageable.getPageNumber())
                .isEqualTo(2);

        assertThat(pageable.getPageSize())
                .isEqualTo(10);

        assertThat(
                pageable
                        .getSort()
                        .getOrderFor("createdAt")
        )
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void invalidCartSortReturnsValidationError()
            throws Exception {
        authenticate(1L);

        mockMvc.perform(
                        get("/api/cart-items")
                                .param(
                                        "sort",
                                        "name,desc"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error.code")
                                .value("VALIDATION_ERROR")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].field")
                                .value("sort")
                )
                .andExpect(
                        jsonPath("$.error.fields[0].reason")
                                .value(
                                        "지원하지 않는 정렬 조건입니다."
                                )
                );
    }

    private void authenticate(
            Long userId
    ) {
        Jwt jwt =
                Jwt.withTokenValue("test-token")
                        .header(
                                "alg",
                                "HS256"
                        )
                        .subject(
                                String.valueOf(userId)
                        )
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