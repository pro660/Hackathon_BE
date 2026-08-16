package org.likelionhsu.hackathon.imageasset.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.security.TrustedOriginFilter;
import org.likelionhsu.hackathon.common.exception.GlobalExceptionHandler;
import org.likelionhsu.hackathon.imageasset.dto.response.ImageAssetUploadResponse;
import org.likelionhsu.hackathon.imageasset.service.ImageAssetService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(
        controllers = ImageAssetController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TrustedOriginFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        ImageAssetControllerTest
                .SecurityArgumentResolverConfig.class
})
class ImageAssetControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageAssetService imageAssetService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadReturns201() throws Exception {
        authenticate(USER_ID);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "item.jpg",
                        "image/jpeg",
                        new byte[] {1, 2, 3}
                );

        when(imageAssetService
                .uploadTemporaryItemImage(
                        eq(USER_ID),
                        any()
                ))
                .thenReturn(
                        new ImageAssetUploadResponse(
                                "51",
                                "https://example.com/51.jpg"
                        )
                );

        mockMvc.perform(
                        multipart("/api/image-assets")
                                .file(file)
                )
                .andExpect(status().isCreated())
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

        verify(imageAssetService)
                .uploadTemporaryItemImage(
                        eq(USER_ID),
                        any()
                );
    }

    @Test
    void missingFilePartReturns400ImageFileInvalid()
            throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        multipart("/api/image-assets")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("IMAGE_FILE_INVALID"));

        verifyNoInteractions(imageAssetService);
    }

    @Test
    void temporaryDeleteReturns204()
            throws Exception {
        authenticate(USER_ID);

        mockMvc.perform(
                        delete("/api/image-assets/51")
                )
                .andExpect(status().isNoContent());

        verify(imageAssetService)
                .deleteTemporaryItemImage(
                        USER_ID,
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
