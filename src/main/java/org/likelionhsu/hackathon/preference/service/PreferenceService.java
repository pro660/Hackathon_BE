package org.likelionhsu.hackathon.preference.service;

import org.hibernate.exception.ConstraintViolationException;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.preference.dto.request.PreferenceRequest;
import org.likelionhsu.hackathon.preference.dto.response.PreferenceResponse;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.preference.repository.PreferenceRepository;
import org.likelionhsu.hackathon.preference.validation.PreferenceRequestValidator;
import org.likelionhsu.hackathon.preference.validation.PreferenceRequestValidator.ValidatedPreferenceRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;

@Service
public class PreferenceService {

    private static final String
            PREFERENCE_USER_UNIQUE_CONSTRAINT =
            "uk_preference_profiles_user_id";

    private static final String
            PREFERENCE_USER_QUALIFIED_UNIQUE_CONSTRAINT =
            "preference_profiles."
                    + PREFERENCE_USER_UNIQUE_CONSTRAINT;

    private final UserRepository userRepository;
    private final PreferenceRepository preferenceRepository;
    private final PreferenceRequestValidator requestValidator;
    private final PreferenceWriteTransactionService
            writeTransactionService;

    public PreferenceService(
            UserRepository userRepository,
            PreferenceRepository preferenceRepository,
            PreferenceRequestValidator requestValidator,
            PreferenceWriteTransactionService
                    writeTransactionService
    ) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.requestValidator = requestValidator;
        this.writeTransactionService =
                writeTransactionService;
    }

    @Transactional(readOnly = true)
    public PreferenceResponse getPreference(
            Long userId
    ) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ACCESS_TOKEN_INVALID
                        )
                );

        ensureActive(user);

        return preferenceRepository
                .findByUser_Id(userId)
                .map(this::toResponse)
                .orElse(null);
    }

    public PreferenceResponse updatePreference(
            Long userId,
            PreferenceRequest request
    ) {
        ValidatedPreferenceRequest validatedRequest =
                requestValidator.validate(request);

        try {
            return executeWrite(
                    userId,
                    validatedRequest
            );
        } catch (RuntimeException exception) {
            if (!isRetryableConcurrencyFailure(
                    exception
            )) {
                throw exception;
            }
        }

        try {
            return executeWrite(
                    userId,
                    validatedRequest
            );
        } catch (RuntimeException exception) {
            if (isRetryableConcurrencyFailure(
                    exception
            )) {
                throw new BusinessException(
                        ErrorCode.PREFERENCE_UPDATE_CONFLICT
                );
            }

            throw exception;
        }
    }

    private PreferenceResponse executeWrite(
            Long userId,
            ValidatedPreferenceRequest request
    ) {
        PreferenceProfile profile =
                writeTransactionService.execute(
                        userId,
                        request
                );

        return toResponse(profile);
    }

    private boolean isRetryableConcurrencyFailure(
            Throwable exception
    ) {
        Throwable current = exception;

        while (current != null) {
            if (current
                    instanceof OptimisticLockingFailureException
                    || current
                    instanceof OptimisticLockException) {

                return true;
            }

            if (current
                    instanceof ConstraintViolationException
                    constraintViolationException
                    && isPreferenceUserUniqueConstraint(
                    constraintViolationException
                            .getConstraintName()
            )) {

                return true;
            }

            Throwable cause = current.getCause();

            if (cause == current) {
                break;
            }

            current = cause;
        }

        return false;

    }

    private boolean isPreferenceUserUniqueConstraint(
            String constraintName
    ) {
        return PREFERENCE_USER_UNIQUE_CONSTRAINT
                .equals(constraintName)
                || PREFERENCE_USER_QUALIFIED_UNIQUE_CONSTRAINT
                .equals(constraintName);
    }

    private void ensureActive(
            User user
    ) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_NOT_ACTIVE
            );
        }
    }

    private PreferenceResponse toResponse(
            PreferenceProfile profile
    ) {
        return new PreferenceResponse(
                profile.getPreferredColors()
                        .stream()
                        .map(Enum::name)
                        .toList(),
                profile.getPreferredCategories()
                        .stream()
                        .map(Enum::name)
                        .toList(),
                profile.getPreferredStyleTags()
                        .stream()
                        .map(Enum::name)
                        .toList()
        );
    }
}