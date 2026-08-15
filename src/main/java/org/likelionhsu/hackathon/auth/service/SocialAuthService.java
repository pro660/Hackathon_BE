package org.likelionhsu.hackathon.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.likelionhsu.hackathon.auth.config.OAuthProperties;
import org.likelionhsu.hackathon.auth.domain.PendingSocialSignup;
import org.likelionhsu.hackathon.auth.domain.SocialAccount;
import org.likelionhsu.hackathon.auth.domain.TermsAgreement;
import org.likelionhsu.hackathon.auth.domain.TermsType;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.dto.request.SocialSignupRequest;
import org.likelionhsu.hackathon.auth.dto.request.TermsAgreementRequest;
import org.likelionhsu.hackathon.auth.dto.response.AuthTokenResponse;
import org.likelionhsu.hackathon.auth.dto.response.AuthenticatedUserResponse;
import org.likelionhsu.hackathon.auth.oauth.OAuthProfile;
import org.likelionhsu.hackathon.auth.repository.PendingSocialSignupRepository;
import org.likelionhsu.hackathon.auth.repository.SocialAccountRepository;
import org.likelionhsu.hackathon.auth.repository.TermsAgreementRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.auth.support.SecureTokenGenerator;
import org.likelionhsu.hackathon.auth.support.TokenHashService;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialAuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PendingSocialSignupRepository pendingRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final AuthTokenService authTokenService;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHashService tokenHashService;
    private final OAuthProperties oauthProperties;
    private final Clock clock;

    public SocialAuthService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            PendingSocialSignupRepository pendingRepository,
            TermsAgreementRepository termsAgreementRepository,
            AuthTokenService authTokenService,
            SecureTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            OAuthProperties oauthProperties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.pendingRepository = pendingRepository;
        this.termsAgreementRepository = termsAgreementRepository;
        this.authTokenService = authTokenService;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashService = tokenHashService;
        this.oauthProperties = oauthProperties;
        this.clock = clock;
    }

    @Transactional
    public CallbackResult process(OAuthProfile profile) {
        return socialAccountRepository
                .findWithUserByProviderAndProviderUserId(
                        profile.provider(),
                        profile.providerUserId()
                )
                .map(account -> existingUser(account.getUser()))
                .orElseGet(() -> startOnboarding(profile));
    }

    @Transactional
    public SignupResult signup(
            String rawOnboardingToken,
            SocialSignupRequest request
    ) {
        if (rawOnboardingToken == null
                || rawOnboardingToken.isBlank()) {
            throw new BusinessException(ErrorCode.SIGNUP_TOKEN_INVALID);
        }

        validateSignupRequest(request);

        Instant now = Instant.now(clock);
        PendingSocialSignup pending = pendingRepository
                .findByOnboardingTokenHashForUpdate(
                        tokenHashService.sha256(rawOnboardingToken)
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.SIGNUP_TOKEN_INVALID
                        )
                );

        if (!pending.isUsableAt(now)) {
            throw new BusinessException(ErrorCode.SIGNUP_TOKEN_INVALID);
        }

        if (socialAccountRepository
                .existsByProviderAndProviderUserId(
                        pending.getProvider(),
                        pending.getProviderUserId()
                )) {
            throw new BusinessException(
                    ErrorCode.SOCIAL_ACCOUNT_ALREADY_EXISTS
            );
        }

        ensureEmailDoesNotConflict(pending.getProviderEmail());

        User user = userRepository.saveAndFlush(
                User.social(
                        request.nickname().trim(),
                        request.gender(),
                        normalizeEmail(request.notificationEmail())
                )
        );

        try {
            socialAccountRepository.saveAndFlush(
                    new SocialAccount(
                            user,
                            pending.getProvider(),
                            pending.getProviderUserId(),
                            pending.getProviderEmail()
                    )
            );
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    ErrorCode.SOCIAL_ACCOUNT_ALREADY_EXISTS
            );
        }

        request.termsAgreements().stream()
                .map(agreement -> new TermsAgreement(
                        user,
                        agreement.termsType(),
                        agreement.termsVersion(),
                        agreement.agreed(),
                        now
                ))
                .forEach(termsAgreementRepository::save);

        pending.consume(now);

        AuthTokenService.IssuedTokens tokens =
                authTokenService.issue(user);
        return new SignupResult(
                new AuthTokenResponse(
                        tokens.accessToken(),
                        "Bearer",
                        tokens.accessTokenExpiresInSeconds(),
                        new AuthenticatedUserResponse(
                                user.getId().toString(),
                                user.getNickname(),
                                user.getGender()
                        )
                ),
                tokens.refreshToken()
        );
    }

    private CallbackResult existingUser(User user) {
        ensureActive(user);
        return CallbackResult.existing(
                authTokenService.issueRefreshToken(user)
        );
    }

    private CallbackResult startOnboarding(OAuthProfile profile) {
        String providerEmail = normalizeEmail(profile.providerEmail());
        ensureEmailDoesNotConflict(providerEmail);

        Instant now = Instant.now(clock);
        String rawOnboardingToken = tokenGenerator.generateToken();
        String tokenHash = tokenHashService.sha256(rawOnboardingToken);
        Instant expiresAt = now.plus(
                oauthProperties.onboardingTokenTtl()
        );

        PendingSocialSignup pending = pendingRepository
                .findByProviderAndProviderUserId(
                        profile.provider(),
                        profile.providerUserId()
                )
                .orElseGet(() -> new PendingSocialSignup(
                        profile.provider(),
                        profile.providerUserId(),
                        providerEmail,
                        tokenHash,
                        expiresAt
                ));

        if (pending.getId() != null) {
            pending.renew(providerEmail, tokenHash, expiresAt);
        }
        pendingRepository.saveAndFlush(pending);

        return CallbackResult.onboarding(rawOnboardingToken);
    }

    private void validateSignupRequest(SocialSignupRequest request) {
        String nickname = request.nickname().trim();
        if (nickname.length() < 2 || nickname.length() > 20) {
            throw new BusinessException(ErrorCode.PROFILE_INCOMPLETE);
        }

        Map<TermsType, TermsAgreementRequest> agreements =
                new EnumMap<>(TermsType.class);
        for (TermsAgreementRequest agreement : request.termsAgreements()) {
            if (agreements.put(agreement.termsType(), agreement) != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }

        if (!isAgreed(agreements, TermsType.SERVICE_TERMS)
                || !isAgreed(agreements, TermsType.PRIVACY_POLICY)) {
            throw new BusinessException(
                    ErrorCode.REQUIRED_TERMS_NOT_AGREED
            );
        }
    }

    private boolean isAgreed(
            Map<TermsType, TermsAgreementRequest> agreements,
            TermsType termsType
    ) {
        TermsAgreementRequest agreement = agreements.get(termsType);
        return agreement != null && agreement.agreed();
    }

    private void ensureEmailDoesNotConflict(String providerEmail) {
        if (providerEmail == null) {
            return;
        }

        if (userRepository.existsByEmail(providerEmail)
                || socialAccountRepository
                .existsByProviderEmailIgnoreCase(providerEmail)) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_CONFLICT);
        }
    }

    private void ensureActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank()
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }

    public record CallbackResult(
            CallbackType type,
            String refreshToken,
            String onboardingToken
    ) {

        static CallbackResult existing(String refreshToken) {
            return new CallbackResult(
                    CallbackType.EXISTING_USER,
                    refreshToken,
                    null
            );
        }

        static CallbackResult onboarding(String onboardingToken) {
            return new CallbackResult(
                    CallbackType.ONBOARDING_REQUIRED,
                    null,
                    onboardingToken
            );
        }
    }

    public enum CallbackType {
        EXISTING_USER,
        ONBOARDING_REQUIRED
    }

    public record SignupResult(
            AuthTokenResponse response,
            String refreshToken
    ) {
    }
}
