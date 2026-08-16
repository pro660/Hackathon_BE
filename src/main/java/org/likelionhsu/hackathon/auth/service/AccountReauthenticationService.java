package org.likelionhsu.hackathon.auth.service;

import java.time.Clock;
import java.time.Instant;

import org.likelionhsu.hackathon.auth.config.ReauthenticationProperties;
import org.likelionhsu.hackathon.auth.domain.LocalCredential;
import org.likelionhsu.hackathon.auth.domain.ReauthToken;
import org.likelionhsu.hackathon.auth.domain.ReauthTokenPurpose;
import org.likelionhsu.hackathon.auth.domain.SocialAccount;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.oauth.OAuthProfile;
import org.likelionhsu.hackathon.auth.repository.LocalCredentialRepository;
import org.likelionhsu.hackathon.auth.repository.ReauthTokenRepository;
import org.likelionhsu.hackathon.auth.repository.SocialAccountRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.auth.support.SecureTokenGenerator;
import org.likelionhsu.hackathon.auth.support.TokenHashService;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountReauthenticationService {

    private static final ReauthTokenPurpose PURPOSE =
            ReauthTokenPurpose.ACCOUNT_DELETE;

    private final UserRepository userRepository;
    private final LocalCredentialRepository
            localCredentialRepository;
    private final SocialAccountRepository
            socialAccountRepository;
    private final ReauthTokenRepository reauthTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHashService tokenHashService;
    private final ReauthenticationProperties properties;
    private final Clock clock;

    public AccountReauthenticationService(
            UserRepository userRepository,
            LocalCredentialRepository localCredentialRepository,
            SocialAccountRepository socialAccountRepository,
            ReauthTokenRepository reauthTokenRepository,
            PasswordEncoder passwordEncoder,
            SecureTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            ReauthenticationProperties properties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.localCredentialRepository =
                localCredentialRepository;
        this.socialAccountRepository =
                socialAccountRepository;
        this.reauthTokenRepository = reauthTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashService = tokenHashService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public String reauthenticateWithPassword(
            Long userId,
            String password
    ) {
        User user = findActiveUser(userId);
        LocalCredential credential = localCredentialRepository
                .findWithUserByUserId(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode
                                        .REAUTHENTICATION_METHOD_NOT_AVAILABLE
                        )
                );

        if (!passwordEncoder.matches(
                password,
                credential.getPasswordHash()
        )) {
            throw new BusinessException(
                    ErrorCode.REAUTHENTICATION_FAILED
            );
        }

        return issue(user);
    }

    @Transactional
    public String reauthenticateSocial(OAuthProfile profile) {
        SocialAccount account = socialAccountRepository
                .findWithUserByProviderAndProviderUserId(
                        profile.provider(),
                        profile.providerUserId()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.REAUTHENTICATION_FAILED
                        )
                );

        User user = account.getUser();
        ensureActive(user);
        return issue(user);
    }

    @Transactional
    public void consume(
            Long userId,
            String rawToken
    ) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }

        Instant now = Instant.now(clock);
        ReauthToken token = reauthTokenRepository
                .findByTokenHashForUpdate(
                        tokenHashService.sha256(rawToken)
                )
                .orElseThrow(this::invalidToken);

        if (!token.getUser().getId().equals(userId)
                || token.getPurpose() != PURPOSE
                || !token.isUsableAt(now)) {
            throw invalidToken();
        }

        token.consume(now);
        reauthTokenRepository.saveAndFlush(token);
    }

    private String issue(User user) {
        reauthTokenRepository.deleteByUser_IdAndPurpose(
                user.getId(),
                PURPOSE
        );

        String rawToken = tokenGenerator.generateToken();
        Instant expiresAt = Instant.now(clock)
                .plus(properties.tokenTtl());

        reauthTokenRepository.saveAndFlush(
                new ReauthToken(
                        user,
                        PURPOSE,
                        tokenHashService.sha256(rawToken),
                        expiresAt
                )
        );

        return rawToken;
    }

    private User findActiveUser(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ACCESS_TOKEN_INVALID
                        )
                );
        ensureActive(user);
        return user;
    }

    private void ensureActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_NOT_ACTIVE
            );
        }
    }

    private BusinessException invalidToken() {
        return new BusinessException(
                ErrorCode.REAUTH_TOKEN_INVALID
        );
    }
}
