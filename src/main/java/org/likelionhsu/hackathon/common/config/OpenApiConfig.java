package org.likelionhsu.hackathon.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI hackathonOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("입을래? API")
                                .description(
                                        "2026 중앙해커톤 입을래? 백엔드 API"
                                )
                                .version("v1")
                );
    }
}