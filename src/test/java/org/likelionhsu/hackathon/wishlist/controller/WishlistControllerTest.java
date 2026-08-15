package org.likelionhsu.hackathon.wishlist.controller;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.product.entity.ProductBrand;
import org.likelionhsu.hackathon.wishlist.dto.response.WishlistItemResponse;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.wishlist.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(
        controllers = WishlistController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        WishlistControllerTest.SecurityArgumentResolverConfig.class
})
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishlistService wishlistService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void favoriteCanBeAdded() throws Exception {
        authenticate(1L);

        mockMvc.perform(
                        put("/api/products/10/favorite")
                )
                .andExpect(
                        status().isNoContent()
                )
                .andExpect(
                        content().string("")
                );

        verify(wishlistService)
                .addFavorite(
                        1L,
                        10L
                );
    }

    @Test
    void favoriteCanBeRemoved() throws Exception {
        authenticate(1L);

        mockMvc.perform(
                        delete("/api/products/10/favorite")
                )
                .andExpect(
                        status().isNoContent()
                )
                .andExpect(
                        content().string("")
                );

        verify(wishlistService)
                .removeFavorite(
                        1L,
                        10L
                );
    }

    @Test
    void missingProductReturns404WhenAddingFavorite()
            throws Exception {

        authenticate(1L);

        doThrow(
                new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND
                )
        )
                .when(wishlistService)
                .addFavorite(
                        1L,
                        999L
                );

        mockMvc.perform(
                        put("/api/products/999/favorite")
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
                )
                .andExpect(
                        jsonPath("$.error.message")
                                .value("제품을 찾을 수 없습니다.")
                );
    }

    @Test
    void zeroProductIdReturnsValidationError()
            throws Exception {

        authenticate(1L);

        mockMvc.perform(
                        put("/api/products/0/favorite")
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
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
    void wishlistsReturnDefaultPage() throws Exception {
        authenticate(1L);

        PageResponse<WishlistItemResponse> response =
                new PageResponse<>(
                        List.of(
                                new WishlistItemResponse(
                                        "10",
                                        ProductBrand.MCM,
                                        "MCM Black Bag",
                                        ItemCategory.BAG,
                                        1_500_000L,
                                        ColorGroup.BLACK,
                                        "https://example.com/product.webp",
                                        true
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
                wishlistService.getWishlists(
                        eq(1L),
                        any(Pageable.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/wishlists")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
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
                        jsonPath("$.data.items[0].category")
                                .value("BAG")
                )
                .andExpect(
                        jsonPath("$.data.items[0].price")
                                .value(1_500_000)
                )
                .andExpect(
                        jsonPath("$.data.items[0].primaryColor")
                                .value("BLACK")
                )
                .andExpect(
                        jsonPath("$.data.items[0].primaryImageUrl")
                                .value(
                                        "https://example.com/product.webp"
                                )
                )
                .andExpect(
                        jsonPath("$.data.items[0].favorited")
                                .value(true)
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

        verify(wishlistService)
                .getWishlists(
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
    void wishlistsAcceptCustomPageAndSort()
            throws Exception {

        authenticate(1L);

        PageResponse<WishlistItemResponse> response =
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
                wishlistService.getWishlists(
                        eq(1L),
                        any(Pageable.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/api/wishlists")
                                .param(
                                        "page",
                                        "2"
                                )
                                .param(
                                        "size",
                                        "10"
                                )
                                .param(
                                        "sort",
                                        "createdAt,asc"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data.page")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.data.size")
                                .value(10)
                );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(wishlistService)
                .getWishlists(
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
    void negativeWishlistPageReturnsValidationError()
            throws Exception {

        authenticate(1L);

        mockMvc.perform(
                        get("/api/wishlists")
                                .param(
                                        "page",
                                        "-1"
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
                                .value("page")
                );
    }

    @Test
    void oversizedWishlistPageSizeReturnsValidationError()
            throws Exception {

        authenticate(1L);

        mockMvc.perform(
                        get("/api/wishlists")
                                .param(
                                        "size",
                                        "101"
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
                                .value("size")
                );
    }

    @Test
    void invalidWishlistSortReturnsValidationError()
            throws Exception {

        authenticate(1L);

        mockMvc.perform(
                        get("/api/wishlists")
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