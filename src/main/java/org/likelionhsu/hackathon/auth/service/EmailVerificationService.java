package org.likelionhsu.hackathon.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.likelionhsu.hackathon.auth.config.AuthProperties;
import org.likelionhsu.hackathon.auth.domain.EmailVerification;
import org.likelionhsu.hackathon.auth.domain.EmailVerificationPurpose;
import org.likelionhsu.hackathon.auth.dto.request.EmailVerificationConfirmRequest;
import org.likelionhsu.hackathon.auth.dto.request.EmailVerificationRequest;
import org.likelionhsu.hackathon.auth.dto.response.EmailVerificationConfirmResponse;
import org.likelionhsu.hackathon.auth.dto.response.EmailVerificationResponse;
import org.likelionhsu.hackathon.auth.repository.EmailVerificationRepository;
import org.likelionhsu.hackathon.auth.support.EmailVerificationCodeSender;
import org.likelionhsu.hackathon.auth.support.SecureTokenGenerator;
import org.likelionhsu.hackathon.auth.support.TokenHashService;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final EmailVerificationCodeSender codeSender;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHashService tokenHashService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;
    private final Clock clock;

    public EmailVerificationService(
            EmailVerificationRepository verificationRepository,
            EmailVerificationCodeSender codeSender,
            SecureTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            PasswordEncoder passwordEncoder,
            AuthProperties properties,
            Clock clock
    ) {
        this.verificationRepository = verificationRepository;
        this.codeSender = codeSender;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashService = tokenHashService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public EmailVerificationResponse request(
            EmailVerificationRequest request
    ) {
        ensureSignupPurpose(request.purpose());

        String email = normalizeEmail(request.email());
        Instant now = Instant.now(clock);

        verificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        email,
                        request.purpose()
                )
                .filter(latest -> latest.getCreatedAt().plus(
                        properties.emailResendInterval()
                ).isAfter(now))
                .ifPresent(latest -> {
                    throw new BusinessException(
                            ErrorCode.EMAIL_VERIFICATION_RATE_LIMITED
                    );
                });

        long dailyCount = verificationRepository
                .countByEmailAndPurposeAndCreatedAtGreaterThanEqual(
                        email,
                        request.purpose(),
                        now.minusSeconds(86_400)
                );

        if (dailyCount >= properties.emailDailySendLimit()) {
            throw new BusinessException(
                    ErrorCode.EMAIL_VERIFICATION_RATE_LIMITED
            );
        }

        String verificationCode = tokenGenerator.generateSixDigitCode();
        verificationRepository.saveAndFlush(
                EmailVerification.signup(
                        email,
                        passwordEncoder.encode(verificationCode),
                        now.plus(properties.emailCodeTtl())
                )
        );
        codeSender.send(email, verificationCode);

        return new EmailVerificationResponse(
                properties.emailCodeTtl().toSeconds(),
                properties.emailResendInterval().toSeconds()
        );
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public EmailVerificationConfirmResponse confirm(
            EmailVerificationConfirmRequest request
    ) {
        ensureSignupPurpose(request.purpose());

        String email = normalizeEmail(request.email());
        Instant now = Instant.now(clock);
        EmailVerification verification = verificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        email,
                        request.purpose()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.EMAIL_VERIFICATION_INVALID
                        )
                );

        if (!verification.getCodeExpiresAt().isAfter(now)) {
            throw new BusinessException(
                    ErrorCode.EMAIL_VERIFICATION_EXPIRED
            );
        }

        if (!verification.isCodeUsableAt(
                now,
                properties.emailMaxAttempts()
        )) {
            throw new BusinessException(
                    ErrorCode.EMAIL_VERIFICATION_INVALID
            );
        }

        if (!passwordEncoder.matches(
                request.verificationCode(),
                verification.getCodeHash()
        )) {
            verification.recordFailedAttempt();
            throw new BusinessException(
                    ErrorCode.EMAIL_VERIFICATION_INVALID
            );
        }

        String signupToken = tokenGenerator.generateToken();
        verification.completeSignupVerification(
                tokenHashService.sha256(signupToken),
                now,
                now.plus(properties.signupTokenTtl())
        );

        return new EmailVerificationConfirmResponse(
                signupToken,
                properties.signupTokenTtl().toSeconds()
        );
    }

    private void ensureSignupPurpose(
            EmailVerificationPurpose purpose
    ) {
        if (purpose != EmailVerificationPurpose.SIGNUP) {
            throw new BusinessException(
                    ErrorCode.EMAIL_VERIFICATION_INVALID
            );
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
