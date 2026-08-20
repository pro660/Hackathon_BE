package org.likelionhsu.hackathon.user.service;

import java.time.Clock;
import java.time.Instant;

import org.likelionhsu.hackathon.auth.domain.TermsAgreement;
import org.likelionhsu.hackathon.auth.domain.TermsType;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.TermsAgreementRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.user.dto.request.UserNotificationSettingsUpdateRequest;
import org.likelionhsu.hackathon.user.dto.response.UserNotificationSettingsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserNotificationSettingsService {

    private static final String SETTINGS_TERMS_VERSION =
            "settings-v1";

    private final UserRepository userRepository;
    private final TermsAgreementRepository
            termsAgreementRepository;
    private final Clock clock;

    public UserNotificationSettingsService(
            UserRepository userRepository,
            TermsAgreementRepository termsAgreementRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.termsAgreementRepository =
                termsAgreementRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserNotificationSettingsResponse getSettings(
            Long userId
    ) {
        User user = findActiveUser(userId);

        return toResponse(
                user,
                isAgreed(userId, TermsType.PUSH_MARKETING),
                isAgreed(userId, TermsType.EMAIL_MARKETING)
        );
    }

    @Transactional
    public UserNotificationSettingsResponse updateSettings(
            Long userId,
            UserNotificationSettingsUpdateRequest request
    ) {
        User user = findActiveUserForUpdate(userId);

        user.updateNotificationSettings(
                request.careReminderEnabled(),
                request.recommendationUpdateEnabled()
        );

        Instant decidedAt = clock.instant();

        updateAgreement(
                user,
                TermsType.PUSH_MARKETING,
                request.marketingPushEnabled(),
                decidedAt
        );
        updateAgreement(
                user,
                TermsType.EMAIL_MARKETING,
                request.emailMarketingEnabled(),
                decidedAt
        );

        return toResponse(
                user,
                request.marketingPushEnabled(),
                request.emailMarketingEnabled()
        );
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ACCESS_TOKEN_INVALID
                        )
                );

        ensureActive(user);
        return user;
    }

    private User findActiveUserForUpdate(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
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

    private boolean isAgreed(
            Long userId,
            TermsType termsType
    ) {
        return termsAgreementRepository
                .findTopByUserIdAndTermsTypeOrderByIdDesc(
                        userId,
                        termsType
                )
                .map(TermsAgreement::isAgreed)
                .orElse(false);
    }

    private void updateAgreement(
            User user,
            TermsType termsType,
            boolean agreed,
            Instant decidedAt
    ) {
        termsAgreementRepository
                .findTopByUserIdAndTermsTypeOrderByIdDesc(
                        user.getId(),
                        termsType
                )
                .ifPresentOrElse(
                        agreement ->
                                agreement.updateAgreement(
                                        agreed,
                                        decidedAt
                                ),
                        () -> termsAgreementRepository.save(
                                new TermsAgreement(
                                        user,
                                        termsType,
                                        SETTINGS_TERMS_VERSION,
                                        agreed,
                                        decidedAt
                                )
                        )
                );
    }

    private UserNotificationSettingsResponse toResponse(
            User user,
            boolean marketingPushEnabled,
            boolean emailMarketingEnabled
    ) {
        return new UserNotificationSettingsResponse(
                user.isCareReminderEnabled(),
                user.isRecommendationUpdateEnabled(),
                marketingPushEnabled,
                emailMarketingEnabled
        );
    }
}
