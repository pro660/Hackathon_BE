package org.likelionhsu.hackathon.auth.oauth;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.likelionhsu.hackathon.auth.config.OAuthProperties;
import org.likelionhsu.hackathon.auth.domain.SocialProvider;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuthProviderClient {

    private final OAuthProperties properties;
    private final RestClient restClient;

    public OAuthProviderClient(OAuthProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public URI authorizationUri(
            SocialProvider provider,
            String state
    ) {
        OAuthProperties.Provider registration = registration(provider);

        return UriComponentsBuilder
                .fromUriString(registration.authorizationUri())
                .queryParam("response_type", "{responseType}")
                .queryParam("client_id", "{clientId}")
                .queryParam("redirect_uri", "{redirectUri}")
                .queryParam("state", "{state}")
                .encode()
                .buildAndExpand(Map.of(
                        "responseType", "code",
                        "clientId", registration.clientId(),
                        "redirectUri", registration.redirectUri(),
                        "state", state
                ))
                .toUri();
    }
    public OAuthProfile fetchProfile(
            SocialProvider provider,
            String code,
            String state
    ) {
        if (code == null || code.isBlank()) {
            throw providerError();
        }

        try {
            return switch (provider) {
                case NAVER -> fetchNaverProfile(code, state);
                case KAKAO -> fetchKakaoProfile(code);
            };
        } catch (RestClientException | IllegalArgumentException exception) {
            throw providerError();
        }
    }

    private OAuthProfile fetchNaverProfile(String code, String state) {
        OAuthProperties.Provider registration =
                registration(SocialProvider.NAVER);
        MultiValueMap<String, String> form = baseTokenForm(
                registration,
                code
        );
        form.add("state", state);

        OAuthTokenResponse token = requestToken(registration, form);
        NaverProfileResponse response = restClient.get()
                .uri(registration.profileUri())
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token.accessToken()
                )
                .retrieve()
                .body(NaverProfileResponse.class);

        if (response == null
                || !"00".equals(response.resultCode())
                || response.response() == null
                || isBlank(response.response().id())) {
            throw providerError();
        }

        return new OAuthProfile(
                SocialProvider.NAVER,
                response.response().id(),
                response.response().email()
        );
    }

    private OAuthProfile fetchKakaoProfile(String code) {
        OAuthProperties.Provider registration =
                registration(SocialProvider.KAKAO);
        MultiValueMap<String, String> form = baseTokenForm(
                registration,
                code
        );

        OAuthTokenResponse token = requestToken(registration, form);
        KakaoProfileResponse response = restClient.get()
                .uri(registration.profileUri())
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token.accessToken()
                )
                .retrieve()
                .body(KakaoProfileResponse.class);

        if (response == null || response.id() == null) {
            throw providerError();
        }

        return new OAuthProfile(
                SocialProvider.KAKAO,
                response.id().toString(),
                response.account() == null
                        ? null
                        : response.account().email()
        );
    }

    private OAuthTokenResponse requestToken(
            OAuthProperties.Provider registration,
            MultiValueMap<String, String> form
    ) {
        OAuthTokenResponse response = restClient.post()
                .uri(registration.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(OAuthTokenResponse.class);

        if (response == null || isBlank(response.accessToken())) {
            throw providerError();
        }
        return response;
    }

    private MultiValueMap<String, String> baseTokenForm(
            OAuthProperties.Provider registration,
            String code
    ) {
        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", registration.clientId());
        if (!isBlank(registration.clientSecret())) {
            form.add("client_secret", registration.clientSecret());
        }
        form.add("redirect_uri", registration.redirectUri());
        form.add("code", code);
        return form;
    }

    private OAuthProperties.Provider registration(
            SocialProvider provider
    ) {
        OAuthProperties.Provider registration =
                properties.provider(provider);
        if (registration == null
                || isBlank(registration.clientId())
                || isBlank(registration.redirectUri())
                || isBlank(registration.authorizationUri())
                || isBlank(registration.tokenUri())
                || isBlank(registration.profileUri())) {
            throw providerError();
        }
        return registration;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BusinessException providerError() {
        return new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OAuthTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverProfileResponse(
            @JsonProperty("resultcode") String resultCode,
            NaverProfile response
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverProfile(
            String id,
            String email
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoProfileResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount account
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoAccount(String email) {
    }
}
