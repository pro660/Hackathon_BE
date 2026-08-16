package org.likelionhsu.hackathon.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.likelionhsu.hackathon.auth.config.AuthProperties;
import org.likelionhsu.hackathon.auth.domain.RefreshToken;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.RefreshTokenRepository;
import org.likelionhsu.hackathon.auth.support.SecureTokenGenerator;
import org.likelionhsu.hackathon.auth.support.TokenHashService;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHashService tokenHashService;
    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;
    private final Clock clock;

    public AuthTokenService(
            RefreshTokenRepository refreshTokenRepository,
            SecureTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            JwtEncoder jwtEncoder,
            AuthProperties properties,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashService = tokenHashService;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedTokens issue(User user) {
        Instant now = Instant.now(clock);
        String rawRefreshToken = createRefreshToken(user, now);

        return new IssuedTokens(
                createAccessToken(user, now),
                rawRefreshToken,
                properties.accessTokenTtl().toSeconds()
        );
    }

    @Transactional
    public String issueRefreshToken(User user) {
        return createRefreshToken(user, Instant.now(clock));
    }

    @Transactional
    public IssuedTokens rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Instant now = Instant.now(clock);
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashForUpdate(
                        tokenHashService.sha256(rawRefreshToken)
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.REFRESH_TOKEN_INVALID
                        )
                );

        if (!storedToken.isUsableAt(now)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = storedToken.getUser();
        ensureActive(user);
        storedToken.revoke(now);

        return issue(user);
    }

    @Transactional
    public void revoke(String rawRefreshToken, Long authenticatedUserId) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository
                .findByTokenHashForUpdate(
                        tokenHashService.sha256(rawRefreshToken)
                )
                .filter(token -> token.getUser().getId()
                        .equals(authenticatedUserId))
                .ifPresent(token -> token.revoke(Instant.now(clock)));
    }

    private String createAccessToken(User user, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    private String createRefreshToken(User user, Instant now) {
        String rawRefreshToken = tokenGenerator.generateToken();
        refreshTokenRepository.save(
                new RefreshToken(
                        user,
                        tokenHashService.sha256(rawRefreshToken),
                        UUID.randomUUID().toString(),
                        now.plus(properties.refreshTokenTtl())
                )
        );
        return rawRefreshToken;
    }

    private void ensureActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    public record IssuedTokens(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInSeconds
    ) {
    }
}
