package org.likelionhsu.hackathon.preference.service;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.preference.repository.PreferenceRepository;
import org.likelionhsu.hackathon.preference.validation.PreferenceRequestValidator.ValidatedPreferenceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceWriteTransactionService {

    private final UserRepository userRepository;
    private final PreferenceRepository preferenceRepository;

    public PreferenceWriteTransactionService(
            UserRepository userRepository,
            PreferenceRepository preferenceRepository
    ) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PreferenceProfile execute(
            Long userId,
            ValidatedPreferenceRequest request
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
                .map(profile ->
                        updateExisting(
                                profile,
                                request
                        )
                )
                .orElseGet(() ->
                        createNew(
                                user,
                                request
                        )
                );
    }

    private PreferenceProfile updateExisting(
            PreferenceProfile profile,
            ValidatedPreferenceRequest request
    ) {
        profile.applyManualPreferences(
                request.preferredColors(),
                request.preferredCategories(),
                request.preferredStyleTags()
        );

        return profile;
    }

    private PreferenceProfile createNew(
            User user,
            ValidatedPreferenceRequest request
    ) {
        PreferenceProfile profile =
                PreferenceProfile.createManual(
                        user,
                        request.preferredColors(),
                        request.preferredCategories(),
                        request.preferredStyleTags()
                );

        return preferenceRepository.save(profile);
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
}