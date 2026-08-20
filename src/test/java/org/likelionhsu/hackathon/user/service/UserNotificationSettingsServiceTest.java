package org.likelionhsu.hackathon.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.TermsAgreement;
import org.likelionhsu.hackathon.auth.domain.TermsType;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.TermsAgreementRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.user.dto.request.UserNotificationSettingsUpdateRequest;
import org.likelionhsu.hackathon.user.dto.response.UserNotificationSettingsResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserNotificationSettingsServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant NOW =
            Instant.parse("2026-08-20T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private TermsAgreementRepository
            termsAgreementRepository;

    private UserNotificationSettingsService service;

    @BeforeEach
    void setUp() {
        service = new UserNotificationSettingsService(
                userRepository,
                termsAgreementRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void settingsCanBeFetched() {
        User user = createUser();
        TermsAgreement emailAgreement =
                new TermsAgreement(
                        user,
                        TermsType.EMAIL_MARKETING,
                        "signup-v1",
                        true,
                        NOW.minusSeconds(60)
                );

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(termsAgreementRepository
                .findTopByUserIdAndTermsTypeOrderByIdDesc(
                        USER_ID,
                        TermsType.PUSH_MARKETING
                ))
                .thenReturn(Optional.empty());
        when(termsAgreementRepository
                .findTopByUserIdAndTermsTypeOrderByIdDesc(
                        USER_ID,
                        TermsType.EMAIL_MARKETING
                ))
                .thenReturn(Optional.of(emailAgreement));

        UserNotificationSettingsResponse response =
                service.getSettings(USER_ID);

        assertThat(response.careReminderEnabled()).isTrue();
        assertThat(response.recommendationUpdateEnabled())
                .isTrue();
        assertThat(response.marketingPushEnabled()).isFalse();
        assertThat(response.emailMarketingEnabled()).isTrue();
    }

    @Test
    void settingsCanBeUpdatedAndExistingEmailConsentIsReused() {
        User user = createUser();
        TermsAgreement emailAgreement =
                new TermsAgreement(
                        user,
                        TermsType.EMAIL_MARKETING,
                        "signup-v1",
                        true,
                        NOW.minusSeconds(60)
                );

        when(userRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(user));
        when(termsAgreementRepository
                .findTopByUserIdAndTermsTypeOrderByIdDesc(
                        USER_ID,
                        TermsType.PUSH_MARKETING
                ))
                .thenReturn(Optional.empty());
        when(termsAgreementRepository
                .findTopByUserIdAndTermsTypeOrderByIdDesc(
                        USER_ID,
                        TermsType.EMAIL_MARKETING
                ))
                .thenReturn(Optional.of(emailAgreement));

        UserNotificationSettingsResponse response =
                service.updateSettings(
                        USER_ID,
                        new UserNotificationSettingsUpdateRequest(
                                false,
                                true,
                                true,
                                false
                        )
                );

        assertThat(response.careReminderEnabled()).isFalse();
        assertThat(response.recommendationUpdateEnabled())
                .isTrue();
        assertThat(response.marketingPushEnabled()).isTrue();
        assertThat(response.emailMarketingEnabled()).isFalse();
        assertThat(emailAgreement.isAgreed()).isFalse();

        ArgumentCaptor<TermsAgreement> captor =
                ArgumentCaptor.forClass(TermsAgreement.class);
        verify(termsAgreementRepository).save(captor.capture());

        TermsAgreement created = captor.getValue();
        assertThat(created.getTermsType())
                .isEqualTo(TermsType.PUSH_MARKETING);
        assertThat(created.isAgreed()).isTrue();
    }

    private User createUser() {
        User user = User.local(
                "user@example.com",
                "사용자",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
