package org.likelionhsu.hackathon.useritem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.common.response.PageResponse;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemCreateRequest;
import org.likelionhsu.hackathon.useritem.dto.request.UserItemUpdateRequest;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemCreateResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemDetailResponse;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemListItemResponse;
import org.likelionhsu.hackathon.useritem.service.UserItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
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
        controllers = UserItemController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        UserItemControllerTest.SecurityArgumentResolverConfig.class
})
class UserItemControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserItemService userItemService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myItemListReturnsFilteredPage() throws Exception {
        authenticate(USER_ID);

        UserItemListItemResponse item =
                new UserItemListItemResponse(
                        "10",
                        "브라운 토트백",
                        "MCM",
                        ItemCategory.BAG,
                        ColorGroup.BROWN,
                        MaterialGroup.LEATHER,
                        "https://example.com/item.webp",
                        Instant.parse("2026-08-16T00:00:00Z")
                );

        when(userItemService.getMyItems(
                eq(USER_ID),
                eq("토트"),
                eq(ItemCategory.BAG),
                eq(ColorGroup.BROWN),
                any(Pageable.class)
        )).thenReturn(
                new PageResponse<>(
                        List.of(item),
                        0,
                        20,
                        1,
                        1,
                        false,
                        false
                )
        );

        mockMvc.perform(
                        get("/api/my-items")
                                .param("keyword", "토트")
                                .param("category", "BAG")
                                .param("color", "BROWN")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].myItemId")
                        .value("10"))
                .andExpect(jsonPath("$.data.items[0].primaryImageUrl")
                        .value("https://example.com/item.webp"));
    }

    @Test
    void myItemCanBeCreatedWithoutImage() throws Exception {
        authenticate(USER_ID);

        when(userItemService.createMyItem(
                eq(USER_ID),
                any(UserItemCreateRequest.class)
        )).thenReturn(new UserItemCreateResponse("10"));

        mockMvc.perform(
                        post("/api/my-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "브라운 토트백",
                                          "category": "BAG",
                                          "primaryColor": "BROWN"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.myItemId").value("10"));
    }

    @Test
    void invalidCreateRequestReturns400() throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        post("/api/my-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(userItemService);
    }

    @Test
    void missingOwnedItemReturns404() throws Exception {
        authenticate(USER_ID);

        when(userItemService.getMyItem(USER_ID, 999L))
                .thenThrow(
                        new BusinessException(
                                ErrorCode.MY_ITEM_NOT_FOUND
                        )
                );

        mockMvc.perform(get("/api/my-items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code")
                        .value("MY_ITEM_NOT_FOUND"));
    }

    @Test
    void itemPatchReturnsUpdatedDetail() throws Exception {
        authenticate(USER_ID);

        when(userItemService.updateMyItem(
                eq(USER_ID),
                eq(10L),
                any(UserItemUpdateRequest.class)
        )).thenReturn(detailResponse());

        mockMvc.perform(
                        patch("/api/my-items/10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "memo": null,
                                          "version": 2
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myItemId")
                        .value("10"))
                .andExpect(jsonPath("$.data.version").value(3));
    }

    @Test
    void missingPatchVersionReturns400() throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        patch("/api/my-items/10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "수정 이름"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(userItemService);
    }

    @Test
    void invalidSortReturns400() throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        get("/api/my-items")
                                .param("sort", "version,desc")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field")
                        .value("sort"));

        verifyNoInteractions(userItemService);
    }

    @Test
    void itemDeleteReturns204() throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(delete("/api/my-items/10"))
                .andExpect(status().isNoContent());

        verify(userItemService).deleteMyItem(USER_ID, 10L);
    }

    private UserItemDetailResponse detailResponse() {
        return new UserItemDetailResponse(
                "10",
                null,
                "MCM",
                "브라운 토트백",
                ItemCategory.BAG,
                ColorGroup.BROWN,
                MaterialGroup.LEATHER,
                MaterialSource.USER_CONFIRMED,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                3L,
                Instant.parse("2026-08-16T00:00:00Z"),
                Instant.parse("2026-08-16T01:00:00Z")
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
            resolvers.add(
                    new AuthenticationPrincipalArgumentResolver()
            );
        }
    }
}
