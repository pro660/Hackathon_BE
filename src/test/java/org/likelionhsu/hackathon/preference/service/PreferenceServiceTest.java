package org.likelionhsu.hackathon.preference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.preference.dto.request.PreferenceRequest;
import org.likelionhsu.hackathon.preference.dto.response.PreferenceResponse;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.preference.entity.PreferenceStyleTag;
import org.likelionhsu.hackathon.preference.repository.PreferenceRepository;
import org.likelionhsu.hackathon.preference.validation.PreferenceRequestValidator;
import org.likelionhsu.hackathon.preference.validation.PreferenceRequestValidator.ValidatedPreferenceRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PreferenceRepository preferenceRepository;

    @Mock
    private PreferenceRequestValidator requestValidator;

    @Mock
    private PreferenceWriteTransactionService
            writeTransactionService;

    private PreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService =
                new PreferenceService(
                        userRepository,
                        preferenceRepository,
                        requestValidator,
                        writeTransactionService
                );
    }

    @Test
    void existingPreferenceCanBeFetched() {
        User user = createUser();

        PreferenceProfile profile =
                createProfile(user);

        when(
                userRepository.findById(USER_ID)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                preferenceRepository.findByUser_Id(
                        USER_ID
                )
        ).thenReturn(
                Optional.of(profile)
        );

        PreferenceResponse response =
                preferenceService.getPreference(
                        USER_ID
                );

        assertThat(response.preferredColors())
                .containsExactly("BLACK");

        assertThat(response.preferredCategories())
                .containsExactly("BAG");

        assertThat(response.preferredStyleTags())
                .containsExactly("CASUAL");
    }

    @Test
    void missingPreferenceReturnsNull() {
        when(
                userRepository.findById(USER_ID)
        ).thenReturn(
                Optional.of(createUser())
        );

        when(
                preferenceRepository.findByUser_Id(
                        USER_ID
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThat(
                preferenceService.getPreference(
                        USER_ID
                )
        ).isNull();
    }

    @Test
    void missingJwtUserThrowsAccessTokenInvalid() {
        when(
                userRepository.findById(USER_ID)
        ).thenReturn(
                Optional.empty()
        );

        assertBusinessException(
                () ->
                        preferenceService
                                .getPreference(
                                        USER_ID
                                ),
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

        when(
                userRepository.findById(USER_ID)
        ).thenReturn(
                Optional.of(user)
        );

        assertBusinessException(
                () ->
                        preferenceService
                                .getPreference(
                                        USER_ID
                                ),
                ErrorCode.ACCOUNT_NOT_ACTIVE
        );
    }

    @Test
    void updateWithoutConflictUsesOneWriteAttempt() {
        PreferenceRequest request =
                request();

        ValidatedPreferenceRequest validated =
                validatedRequest();

        when(
                requestValidator.validate(request)
        ).thenReturn(validated);

        when(
                writeTransactionService.execute(
                        USER_ID,
                        validated
                )
        ).thenReturn(
                createProfile(
                        createUser()
                )
        );

        PreferenceResponse response =
                preferenceService.updatePreference(
                        USER_ID,
                        request
                );

        assertThat(response.preferredColors())
                .containsExactly("BLACK");

        verify(
                writeTransactionService,
                times(1)
        ).execute(
                USER_ID,
                validated
        );
    }

    @Test
    void firstOptimisticConflictThenSuccessUsesTwoAttempts() {
        PreferenceRequest request =
                request();

        ValidatedPreferenceRequest validated =
                validatedRequest();

        when(
                requestValidator.validate(request)
        ).thenReturn(validated);

        when(
                writeTransactionService.execute(
                        USER_ID,
                        validated
                )
        )
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "optimistic conflict"
                        )
                )
                .thenReturn(
                        createProfile(
                                createUser()
                        )
                );

        preferenceService.updatePreference(
                USER_ID,
                request
        );

        verify(
                writeTransactionService,
                times(2)
        ).execute(
                USER_ID,
                validated
        );
    }

    @Test
    void firstExactUniqueConflictThenSuccessUsesTwoAttempts() {
        PreferenceRequest request =
                request();

        ValidatedPreferenceRequest validated =
                validatedRequest();

        when(
                requestValidator.validate(request)
        ).thenReturn(validated);

        DataIntegrityViolationException firstFailure =
                uniqueConstraintFailure(
                        "uk_preference_profiles_user_id"
                );

        when(
                writeTransactionService.execute(
                        USER_ID,
                        validated
                )
        )
                .thenThrow(firstFailure)
                .thenReturn(
                        createProfile(
                                createUser()
                        )
                );

        preferenceService.updatePreference(
                USER_ID,
                request
        );

        verify(
                writeTransactionService,
                times(2)
        ).execute(
                USER_ID,
                validated
        );
    }

    @Test
    void twoOptimisticConflictsBecomePreferenceUpdateConflict() {
        PreferenceRequest request =
                request();

        ValidatedPreferenceRequest validated =
                validatedRequest();

        when(
                requestValidator.validate(request)
        ).thenReturn(validated);

        when(
                writeTransactionService.execute(
                        USER_ID,
                        validated
                )
        )
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "first conflict"
                        )
                )
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "second conflict"
                        )
                );

        assertBusinessException(
                () ->
                        preferenceService
                                .updatePreference(
                                        USER_ID,
                                        request
                                ),
                ErrorCode.PREFERENCE_UPDATE_CONFLICT
        );

        verify(
                writeTransactionService,
                times(2)
        ).execute(
                USER_ID,
                validated
        );
    }

    @Test
    void twoExactUniqueConflictsBecomePreferenceUpdateConflict() {
        PreferenceRequest request =
                request();

        ValidatedPreferenceRequest validated =
                validatedRequest();

        when(
                requestValidator.validate(request)
        ).thenReturn(validated);

        DataIntegrityViolationException firstFailure =
                uniqueConstraintFailure(
                        "uk_preference_profiles_user_id"
                );

        DataIntegrityViolationException secondFailure =
                uniqueConstraintFailure(
                        "uk_preference_profiles_user_id"
                );

        when(
                writeTransactionService.execute(
                        USER_ID,
                        validated
                )
        )
                .thenThrow(firstFailure)
                .thenThrow(secondFailure);

        assertBusinessException(
                () ->
                        preferenceService
                                .updatePreference(
                                        USER_ID,
                                        request
                                ),
                ErrorCode.PREFERENCE_UPDATE_CONFLICT
        );

        verify(
                writeTransactionService,
                times(2)
        ).execute(
                USER_ID,
                validated
        );
    }

    @Test
    void secondDifferentFailureIsPropagatedNormally() {
        PreferenceRequest request =
                request();

        ValidatedPreferenceRequest validated =
                validatedRequest();

        BusinessException secondFailure =
                new BusinessException(
                        ErrorCode.ACCOUNT_NOT_ACTIVE
                );

        when(
                requestValidator.validate(request)
        ).thenReturn(validated);

        when(
                writeTransactionService.execute(
                        USER_ID,
                        validated
                )
        )
                .thenThrow(
                        new OptimisticLockingFailureException(
                                "first conflict"
                        )
                )
                .thenThrow(secondFailure);

        assertThatThrownBy(
                () ->
                        preferenceService
                                .updatePreference(
                                        USER_ID,
                                        request
                                )
        ).isSameAs(secondFailure);

        verify(
                writeTransactionService,
                times(2)
        ).execute(
                USER_ID,
                validated
        );
    }

    @Test
    void unrelatedDataIntegrityViolationIsNotRetried() {
        PreferenceRequest request =
                request();

        ValidatedPreferenceRequest validated =
                validatedRequest();

        DataIntegrityViolationException failure =
                new DataIntegrityViolationException(
                        "unrelated integrity failure"
                );

        when(
                requestValidator.validate(request)
        ).thenReturn(validated);

        when(
                writeTransactionService.execute(
                        USER_ID,
                        validated
                )
        ).thenThrow(failure);

        assertThatThrownBy(
                () ->
                        preferenceService
                                .updatePreference(
                                        USER_ID,
                                        request
                                )
        ).isSameAs(failure);

        verify(
                writeTransactionService,
                times(1)
        ).execute(
                USER_ID,
                validated
        );
    }

    @Test
    void qualifiedExactUniqueConflictThenSuccessUsesTwoAttempts() {
        PreferenceRequest request =
                request();

        ValidatedPreferenceRequest validated =
                validatedRequest();

        DataIntegrityViolationException firstFailure =
                uniqueConstraintFailure(
                        "preference_profiles."
                                + "uk_preference_profiles_user_id"
                );

        when(
                requestValidator.validate(request)
        ).thenReturn(validated);

        when(
                writeTransactionService.execute(
                        USER_ID,
                        validated
                )
        )
                .thenThrow(firstFailure)
                .thenReturn(
                        createProfile(
                                createUser()
                        )
                );

        preferenceService.updatePreference(
                USER_ID,
                request
        );

        verify(
                writeTransactionService,
                times(2)
        ).execute(
                USER_ID,
                validated
        );
    }

    @Test
    void differentUniqueConstraintIsNotRetried() {
        PreferenceRequest request =
                request();

        ValidatedPreferenceRequest validated =
                validatedRequest();

        DataIntegrityViolationException failure =
                uniqueConstraintFailure(
                        "uk_wishlists_user_product"
                );

        when(
                requestValidator.validate(request)
        ).thenReturn(validated);

        when(
                writeTransactionService.execute(
                        USER_ID,
                        validated
                )
        ).thenThrow(failure);

        assertThatThrownBy(
                () ->
                        preferenceService
                                .updatePreference(
                                        USER_ID,
                                        request
                                )
        ).isSameAs(failure);

        verify(
                writeTransactionService,
                times(1)
        ).execute(
                USER_ID,
                validated
        );
    }

    private DataIntegrityViolationException
    uniqueConstraintFailure(
            String constraintName
    ) {
        ConstraintViolationException cause =
                mock(
                        ConstraintViolationException.class
                );

        when(
                cause.getConstraintName()
        ).thenReturn(constraintName);

        return new DataIntegrityViolationException(
                "constraint violation",
                cause
        );
    }

    private PreferenceRequest request() {
        return new PreferenceRequest(
                List.of("BLACK"),
                List.of("BAG"),
                List.of("CASUAL")
        );
    }

    private ValidatedPreferenceRequest
    validatedRequest() {
        return new ValidatedPreferenceRequest(
                List.of(
                        ColorGroup.BLACK
                ),
                List.of(
                        ItemCategory.BAG
                ),
                List.of(
                        PreferenceStyleTag.CASUAL
                )
        );
    }

    private User createUser() {
        return User.local(
                "preference-service@example.com",
                "preference-service-user",
                Gender.NOT_SPECIFIED
        );
    }

    private PreferenceProfile createProfile(
            User user
    ) {
        return PreferenceProfile.createManual(
                user,
                List.of(
                        ColorGroup.BLACK
                ),
                List.of(
                        ItemCategory.BAG
                ),
                List.of(
                        PreferenceStyleTag.CASUAL
                )
        );
    }

    private void assertBusinessException(
            ThrowingRunnable runnable,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(
                runnable::run
        )
                .isInstanceOf(
                        BusinessException.class
                )
                .satisfies(exception -> {
                    BusinessException
                            businessException =
                            (BusinessException)
                                    exception;

                    assertThat(
                            businessException
                                    .getErrorCode()
                    ).isEqualTo(
                            expectedErrorCode
                    );
                });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run();
    }
}