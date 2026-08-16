package org.likelionhsu.hackathon.useritem.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.useritem.dto.response.UserItemImageLinkResponse;
import org.likelionhsu.hackathon.useritem.service.UserItemImageService;
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
        controllers = UserItemImageController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        UserItemImageControllerTest
                .SecurityArgumentResolverConfig.class
})
class UserItemImageControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserItemImageService userItemImageService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void attachReturns200WithLinkedImage()
            throws Exception {
        authenticate(USER_ID);

        when(userItemImageService.attach(
                USER_ID,
                10L,
                51L
        )).thenReturn(
                new UserItemImageLinkResponse(
                        "51",
                        "https://example.com/51.jpg"
                )
        );

        mockMvc.perform(
                        put(
                                "/api/my-items/10/images/51"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success")
                        .value(true))
                .andExpect(jsonPath(
                        "$.data.imageAssetId"
                ).value("51"))
                .andExpect(jsonPath(
                        "$.data.imageUrl"
                ).value(
                        "https://example.com/51.jpg"
                ));

        verify(userItemImageService).attach(
                USER_ID,
                10L,
                51L
        );
    }

    @Test
    void linkedDeleteReturns204()
            throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        delete(
                                "/api/my-items/10/images/51"
                        )
                )
                .andExpect(status().isNoContent());

        verify(userItemImageService).delete(
                USER_ID,
                10L,
                51L
        );
    }

    private void authenticate(Long userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(String.valueOf(userId))
                .build();

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(jwt)
                );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityArgumentResolverConfig
            implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(
                List<HandlerMethodArgumentResolver>
                        resolvers
        ) {
            resolvers.add(
                    new AuthenticationPrincipalArgumentResolver()
            );
        }
    }
}
