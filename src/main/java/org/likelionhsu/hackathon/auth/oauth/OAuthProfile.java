package org.likelionhsu.hackathon.auth.oauth;

import org.likelionhsu.hackathon.auth.domain.SocialProvider;

public record OAuthProfile(
        SocialProvider provider,
        String providerUserId,
        String providerEmail
) {
}
