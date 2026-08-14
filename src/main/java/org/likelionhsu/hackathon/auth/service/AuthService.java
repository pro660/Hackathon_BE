package org.likelionhsu.hackathon.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.likelionhsu.hackathon.auth.domain.EmailVerification;
import org.likelionhsu.hackathon.auth.domain.LocalCredential;
import org.likelionhsu.hackathon.auth.domain.TermsAgreement;
import org.likelionhsu.hackathon.auth.domain.TermsType;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.dto.request.LoginRequest;
import org.likelionhsu.hackathon.auth.dto.request.SignupRequest;
import org.likelionhsu.hackathon.auth.dto.request.TermsAgreementRequest;
import org.likelionhsu.hackathon.auth.dto.response.AccessTokenResponse;
import org.likelionhsu.hackathon.auth.dto.response.AuthTokenResponse;
import org.likelionhsu.hackathon.auth.dto.response.AuthenticatedUserResponse;
import org.likelionhsu.hackathon.auth.dto.response.LoginIdAvailabilityResponse;
import org.likelionhsu.hackathon.auth.repository.EmailVerificationRepository;
import org.likelionhsu.hackathon.auth.repository.LocalCredentialRepository;
import org.likelionhsu.hackathon.auth.repository.TermsAgreementRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.auth.support.TokenHashService;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository credentialRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final EmailVerificationRepository verificationRepository;
    private final AuthTokenService authTokenService;
    private final TokenHashService tokenHashService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            LocalCredentialRepository credentialRepository,
            TermsAgreementRepository termsAgreementRepository,
            EmailVerificationRepository verificationRepository,
            AuthTokenService authTokenService,
            TokenHashService tokenHashService,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.termsAgreementRepository = termsAgreementRepository;
        this.verificationRepository = verificationRepository;
        this.authTokenService = authTokenService;
        this.tokenHashService = tokenHashService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoginIdAvailabilityResponse checkLoginId(String loginId) {
        String normalizedLoginId = normalizeLoginId(loginId);
        return new LoginIdAvailabilityResponse(
                normalizedLoginId,
                !credentialRepository.existsByLoginId(normalizedLoginId)
        );
    }

    @Transactional
    public AuthResult signup(SignupRequest request) {
        validateSignupRequest(request);

        Instant now = Instant.now(clock);
        EmailVerification verification = verificationRepository
                .findBySignupTokenHashForUpdate(
                        tokenHashService.sha256(request.signupToken())
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.SIGNUP_TOKEN_INVALID)
                );

        if (!verification.isSignupTokenUsableAt(now)) {
            throw new BusinessException(ErrorCode.SIGNUP_TOKEN_INVALID);
        }

        String email = verification.getEmail();
        String loginId = normalizeLoginId(request.loginId());

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (credentialRepository.existsByLoginId(loginId)) {
            throw new BusinessException(
                    ErrorCode.LOGIN_ID_ALREADY_EXISTS
            );
        }

        User user = userRepository.save(
                User.local(email, request.nickname().trim(), request.gender())
        );
        credentialRepository.save(
                new LocalCredential(
                        user,
                        loginId,
                        passwordEncoder.encode(request.password())
                )
        );

        request.termsAgreements().stream()
                .map(agreement -> new TermsAgreement(
                        user,
                        agreement.termsType(),
                        agreement.termsVersion(),
                        agreement.agreed(),
                        now
                ))
                .forEach(termsAgreementRepository::save);

        verification.consumeSignupToken(now);

        return toAuthResult(
                user,
                authTokenService.issue(user)
        );
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        LocalCredential credential = credentialRepository
                .findWithUserByLoginId(
                        normalizeLoginId(request.loginId())
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_CREDENTIALS)
                );

        if (!passwordEncoder.matches(
                request.password(),
                credential.getPasswordHash()
        )) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = credential.getUser();
        ensureActive(user);
        return toAuthResult(user, authTokenService.issue(user));
    }

    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        AuthTokenService.IssuedTokens tokens =
                authTokenService.rotate(rawRefreshToken);

        return new RefreshResult(
                new AccessTokenResponse(
                        tokens.accessToken(),
                        "Bearer",
                        tokens.accessTokenExpiresInSeconds()
                ),
                tokens.refreshToken()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken, Long authenticatedUserId) {
        authTokenService.revoke(rawRefreshToken, authenticatedUserId);
    }

    private void validateSignupRequest(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_CONFIRM_MISMATCH
            );
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

    private AuthResult toAuthResult(
            User user,
            AuthTokenService.IssuedTokens tokens
    ) {
        return new AuthResult(
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

    private void ensureActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    private String normalizeLoginId(String loginId) {
        return loginId.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthResult(
            AuthTokenResponse response,
            String refreshToken
    ) {
    }

    public record RefreshResult(
            AccessTokenResponse response,
            String refreshToken
    ) {
    }
}
