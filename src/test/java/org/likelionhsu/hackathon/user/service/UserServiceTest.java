package org.likelionhsu.hackathon.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.user.dto.request.UserProfileUpdateRequest;
import org.likelionhsu.hackathon.user.dto.response.UserProfileResponse;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void activeUserProfileCanBeFetched() {
        User user = createUser();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        UserProfileResponse response =
                userService.getMyProfile(USER_ID);

        assertThat(response.userId()).isEqualTo("1");
        assertThat(response.nickname()).isEqualTo("기존닉네임");
        assertThat(response.gender()).isEqualTo(Gender.NOT_SPECIFIED);
    }

    @Test
    void nicknameIsTrimmedBeforeUpdate() {
        User user = createUser();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user))
                .thenReturn(user);

        UserProfileResponse response =
                userService.updateMyProfile(
                        USER_ID,
                        new UserProfileUpdateRequest(
                                "  새닉네임  ",
                                null
                        )
                );

        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.gender()).isEqualTo(Gender.NOT_SPECIFIED);
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void genderOnlyCanBeUpdated() {
        User user = createUser();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user))
                .thenReturn(user);

        UserProfileResponse response =
                userService.updateMyProfile(
                        USER_ID,
                        new UserProfileUpdateRequest(
                                null,
                                Gender.FEMALE
                        )
                );

        assertThat(response.nickname()).isEqualTo("기존닉네임");
        assertThat(response.gender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    void emptyUpdateIsRejected() {
        assertThatThrownBy(() ->
                userService.updateMyProfile(
                        USER_ID,
                        new UserProfileUpdateRequest(
                                null,
                                null
                        )
                )
        )
                .isInstanceOfSatisfying(
                        RequestValidationException.class,
                        exception -> assertThat(
                                exception.getField()
                        ).isEqualTo("request")
                );
    }

    @Test
    void nicknameShorterThanTwoAfterTrimIsRejected() {
        assertThatThrownBy(() ->
                userService.updateMyProfile(
                        USER_ID,
                        new UserProfileUpdateRequest(
                                "  가  ",
                                null
                        )
                )
        )
                .isInstanceOfSatisfying(
                        RequestValidationException.class,
                        exception -> assertThat(
                                exception.getField()
                        ).isEqualTo("nickname")
                );
    }

    @Test
    void nicknameWithControlCharacterIsRejected() {
        assertThatThrownBy(() ->
                userService.updateMyProfile(
                        USER_ID,
                        new UserProfileUpdateRequest(
                                "닉네임\n변경",
                                null
                        )
                )
        )
                .isInstanceOfSatisfying(
                        RequestValidationException.class,
                        exception -> assertThat(
                                exception.getField()
                        ).isEqualTo("nickname")
                );
    }

    @Test
    void missingJwtUserThrowsAccessTokenInvalid() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertBusinessException(
                () -> userService.getMyProfile(USER_ID),
                ErrorCode.ACCESS_TOKEN_INVALID
        );
    }

    @Test
    void inactiveUserThrowsAccountNotActive() {
        User user = createUser();
        ReflectionTestUtils.setField(
                user,
                "status",
                UserStatus.SUSPENDED
        );
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        assertBusinessException(
                () -> userService.getMyProfile(USER_ID),
                ErrorCode.ACCOUNT_NOT_ACTIVE
        );
    }

    @Test
    void optimisticConflictBecomesProfileUpdateConflict() {
        User user = createUser();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user))
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "optimistic conflict"
                        )
                );

        assertBusinessException(
                () -> userService.updateMyProfile(
                        USER_ID,
                        new UserProfileUpdateRequest(
                                "새닉네임",
                                null
                        )
                ),
                ErrorCode.USER_PROFILE_UPDATE_CONFLICT
        );
    }

    private User createUser() {
        User user = User.local(
                "user@example.com",
                "기존닉네임",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private void assertBusinessException(
            Runnable action,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode()
                        ).isEqualTo(expectedErrorCode)
                );
    }
}
