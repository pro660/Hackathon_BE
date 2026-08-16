package org.likelionhsu.hackathon.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.config.ReauthenticationProperties;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.LocalCredential;
import org.likelionhsu.hackathon.auth.domain.ReauthToken;
import org.likelionhsu.hackathon.auth.domain.ReauthTokenPurpose;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.LocalCredentialRepository;
import org.likelionhsu.hackathon.auth.repository.ReauthTokenRepository;
import org.likelionhsu.hackathon.auth.repository.SocialAccountRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.auth.support.SecureTokenGenerator;
import org.likelionhsu.hackathon.auth.support.TokenHashService;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountReauthenticationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant NOW =
            Instant.parse("2026-08-16T05:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private LocalCredentialRepository localCredentialRepository;
    @Mock
    private SocialAccountRepository socialAccountRepository;
    @Mock
    private ReauthTokenRepository reauthTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecureTokenGenerator tokenGenerator;
    @Mock
    private TokenHashService tokenHashService;

    private AccountReauthenticationService service;

    @BeforeEach
    void setUp() {
        ReauthenticationProperties properties =
                new ReauthenticationProperties(
                        "http://localhost:3000/success",
                        Duration.ofMinutes(10),
                        new ReauthenticationProperties.Cookie(
                                "account_reauth_token",
                                "/api/users/me",
                                "Lax",
                                false
                        )
                );
        service = new AccountReauthenticationService(
                userRepository,
                localCredentialRepository,
                socialAccountRepository,
                reauthTokenRepository,
                passwordEncoder,
                tokenGenerator,
                tokenHashService,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void correctPasswordIssuesTenMinuteOneTimeToken() {
        User user = user();
        LocalCredential credential = new LocalCredential(
                user,
                "login-id",
                "password-hash"
        );
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(localCredentialRepository
                .findWithUserByUserId(USER_ID))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(
                "password",
                "password-hash"
        )).thenReturn(true);
        when(tokenGenerator.generateToken()).thenReturn("raw-token");
        when(tokenHashService.sha256("raw-token"))
                .thenReturn("token-hash");

        String token = service.reauthenticateWithPassword(
                USER_ID,
                "password"
        );

        assertThat(token).isEqualTo("raw-token");
        verify(reauthTokenRepository)
                .deleteByUser_IdAndPurpose(
                        USER_ID,
                        ReauthTokenPurpose.ACCOUNT_DELETE
                );
        ArgumentCaptor<ReauthToken> captor =
                ArgumentCaptor.forClass(ReauthToken.class);
        verify(reauthTokenRepository).saveAndFlush(
                captor.capture()
        );
        assertThat(captor.getValue().getPurpose())
                .isEqualTo(ReauthTokenPurpose.ACCOUNT_DELETE);
        assertThat(captor.getValue().getTokenHash())
                .isEqualTo("token-hash");
        assertThat(captor.getValue().getExpiresAt())
                .isEqualTo(NOW.plusSeconds(600));
    }

    @Test
    void wrongPasswordIsRejected() {
        User user = user();
        LocalCredential credential = new LocalCredential(
                user,
                "login-id",
                "password-hash"
        );
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(localCredentialRepository
                .findWithUserByUserId(USER_ID))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(
                "wrong-password",
                "password-hash"
        )).thenReturn(false);

        assertError(
                () -> service.reauthenticateWithPassword(
                        USER_ID,
                        "wrong-password"
                ),
                ErrorCode.REAUTHENTICATION_FAILED
        );
    }

    @Test
    void validTokenIsConsumedOnlyForItsUserAndPurpose() {
        User user = user();
        ReauthToken token = new ReauthToken(
                user,
                ReauthTokenPurpose.ACCOUNT_DELETE,
                "token-hash",
                NOW.plusSeconds(600)
        );
        when(tokenHashService.sha256("raw-token"))
                .thenReturn("token-hash");
        when(reauthTokenRepository
                .findByTokenHashForUpdate("token-hash"))
                .thenReturn(Optional.of(token));

        service.consume(USER_ID, "raw-token");

        assertThat(token.getConsumedAt()).isEqualTo(NOW);
        verify(reauthTokenRepository).saveAndFlush(token);
    }

    private User user() {
        User user = User.local(
                "user@example.com",
                "사용자",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private void assertError(
            Runnable action,
            ErrorCode errorCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode()
                        ).isEqualTo(errorCode)
                );
    }
}
