package org.likelionhsu.hackathon.place.client.kakao;

import org.likelionhsu.hackathon.place.client.PlaceSearchPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class KakaoPlaceConfiguration {

    @Bean
    PlaceSearchPort kakaoPlaceSearchPort(
            ObjectMapper objectMapper,
            @Value("${app.place.kakao.rest-api-key:}") String restApiKey
    ) {
        return new KakaoPlaceClient(
                objectMapper,
                restApiKey
        );
    }
}
