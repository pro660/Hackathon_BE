package org.likelionhsu.hackathon.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.LocalCredential;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.dto.request.PasswordChangeRequest;
import org.likelionhsu.hackathon.auth.repository.LocalCredentialRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    private static final Long USER_ID = 1L;
    private static final String CURRENT_PASSWORD = "password123";
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String OLD_HASH = "old-hash";
    private static final String NEW_HASH = "new-hash";

    @Mock
    private UserRepository userRepository;
    @Mock
    private LocalCredentialRepository localCredentialRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordChangeService service;

    @BeforeEach
    void setUp() {
        service = new PasswordChangeService(
                userRepository,
                localCredentialRepository,
                passwordEncoder
        );
    }

    @Test
    void validRequestChangesPasswordWithBCryptHash() {
        User user = activeUser();
        LocalCredential credential = credential(user);
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(localCredentialRepository
                .findWithUserByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH))
                .thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, OLD_HASH))
                .thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn(NEW_HASH);

        service.changePassword(
                USER_ID,
                request(
                        CURRENT_PASSWORD,
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
        );

        assertThat(credential.getPasswordHash()).isEqualTo(NEW_HASH);
        verify(localCredentialRepository)
                .findWithUserByUserIdForUpdate(USER_ID);
        verify(localCredentialRepository).saveAndFlush(credential);
    }

    @Test
    void wrongCurrentPasswordIsRejectedWithoutChangingHash() {
        User user = activeUser();
        LocalCredential credential = credential(user);
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(localCredentialRepository
                .findWithUserByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("wrong123", OLD_HASH))
                .thenReturn(false);

        assertError(
                () -> service.changePassword(
                        USER_ID,
                        request(
                                "wrong123",
                                NEW_PASSWORD,
                                NEW_PASSWORD
                        )
                ),
                ErrorCode.CURRENT_PASSWORD_MISMATCH
        );

        assertThat(credential.getPasswordHash()).isEqualTo(OLD_HASH);
        verify(passwordEncoder, never()).encode(NEW_PASSWORD);
        verify(localCredentialRepository, never())
                .saveAndFlush(credential);
    }

    @Test
    void accountWithoutLocalCredentialCannotChangePassword() {
        User user = activeUser();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(localCredentialRepository
                .findWithUserByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.empty());

        assertError(
                () -> service.changePassword(
                        USER_ID,
                        request(
                                CURRENT_PASSWORD,
                                NEW_PASSWORD,
                                NEW_PASSWORD
                        )
                ),
                ErrorCode.PASSWORD_CHANGE_NOT_AVAILABLE
        );

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void passwordConfirmationMismatchIsRejected() {
        User user = activeUser();
        LocalCredential credential = credential(user);
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(localCredentialRepository
                .findWithUserByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH))
                .thenReturn(true);

        assertError(
                () -> service.changePassword(
                        USER_ID,
                        request(
                                CURRENT_PASSWORD,
                                NEW_PASSWORD,
                                "different123"
                        )
                ),
                ErrorCode.PASSWORD_CONFIRM_MISMATCH
        );

        assertThat(credential.getPasswordHash()).isEqualTo(OLD_HASH);
        verify(passwordEncoder, never()).encode(NEW_PASSWORD);
    }

    @Test
    void samePasswordAsCurrentIsRejected() {
        User user = activeUser();
        LocalCredential credential = credential(user);
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(localCredentialRepository
                .findWithUserByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH))
                .thenReturn(true);

        assertError(
                () -> service.changePassword(
                        USER_ID,
                        request(
                                CURRENT_PASSWORD,
                                CURRENT_PASSWORD,
                                CURRENT_PASSWORD
                        )
                ),
                ErrorCode.NEW_PASSWORD_SAME_AS_CURRENT
        );

        assertThat(credential.getPasswordHash()).isEqualTo(OLD_HASH);
        verify(passwordEncoder, never()).encode(CURRENT_PASSWORD);
    }

    @Test
    void inactiveUserCannotChangePassword() {
        User user = activeUser();
        user.beginDeletion();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        assertError(
                () -> service.changePassword(
                        USER_ID,
                        request(
                                CURRENT_PASSWORD,
                                NEW_PASSWORD,
                                NEW_PASSWORD
                        )
                ),
                ErrorCode.ACCOUNT_NOT_ACTIVE
        );

        verifyNoInteractions(localCredentialRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void missingUserIsRejectedAsInvalidAccessToken() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertError(
                () -> service.changePassword(
                        USER_ID,
                        request(
                                CURRENT_PASSWORD,
                                NEW_PASSWORD,
                                NEW_PASSWORD
                        )
                ),
                ErrorCode.ACCESS_TOKEN_INVALID
        );

        verifyNoInteractions(localCredentialRepository);
        verifyNoInteractions(passwordEncoder);
    }

    private User activeUser() {
        User user = User.local(
                "user@example.com",
                "사용자",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private LocalCredential credential(User user) {
        return new LocalCredential(
                user,
                "login-id",
                OLD_HASH
        );
    }

    private PasswordChangeRequest request(
            String currentPassword,
            String newPassword,
            String newPasswordConfirm
    ) {
        return new PasswordChangeRequest(
                currentPassword,
                newPassword,
                newPasswordConfirm
        );
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
