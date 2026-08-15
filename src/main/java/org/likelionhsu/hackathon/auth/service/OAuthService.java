package org.likelionhsu.hackathon.auth.service;

import java.net.URI;

import org.likelionhsu.hackathon.auth.config.OAuthProperties;
import org.likelionhsu.hackathon.auth.domain.SocialProvider;
import org.likelionhsu.hackathon.auth.oauth.OAuthProfile;
import org.likelionhsu.hackathon.auth.oauth.OAuthProviderClient;
import org.likelionhsu.hackathon.auth.oauth.OAuthStateService;
import org.springframework.stereotype.Service;

@Service
public class OAuthService {

    private final OAuthProviderClient providerClient;
    private final OAuthStateService stateService;
    private final SocialAuthService socialAuthService;
    private final OAuthProperties properties;

    public OAuthService(
            OAuthProviderClient providerClient,
            OAuthStateService stateService,
            SocialAuthService socialAuthService,
            OAuthProperties properties
    ) {
        this.providerClient = providerClient;
        this.stateService = stateService;
        this.socialAuthService = socialAuthService;
        this.properties = properties;
    }

    public StartResult start(String providerPath) {
        SocialProvider provider = SocialProvider.fromPath(providerPath);
        String state = stateService.create();
        return new StartResult(
                state,
                providerClient.authorizationUri(provider, state)
        );
    }

    public SocialAuthService.CallbackResult callback(
            String providerPath,
            String code,
            String queryState,
            String cookieState
    ) {
        SocialProvider provider = SocialProvider.fromPath(providerPath);
        stateService.validate(queryState, cookieState);
        OAuthProfile profile = providerClient.fetchProfile(
                provider,
                code,
                queryState
        );
        return socialAuthService.process(profile);
    }

    public URI successRedirectUri() {
        return URI.create(properties.successUrl());
    }

    public URI onboardingRedirectUri() {
        return URI.create(properties.onboardingUrl());
    }

    public record StartResult(
            String state,
            URI authorizationUri
    ) {
    }
}
