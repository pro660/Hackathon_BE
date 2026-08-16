package org.likelionhsu.hackathon.user.service;

import java.util.ArrayList;
import java.util.List;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.auth.repository.LocalCredentialRepository;
import org.likelionhsu.hackathon.auth.repository.SocialAccountRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.common.exception.RequestValidationException;
import org.likelionhsu.hackathon.user.dto.request.UserProfileUpdateRequest;
import org.likelionhsu.hackathon.user.dto.response.UserProfileResponse;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository
            localCredentialRepository;
    private final SocialAccountRepository
            socialAccountRepository;

    public UserService(
            UserRepository userRepository,
            LocalCredentialRepository localCredentialRepository,
            SocialAccountRepository socialAccountRepository
    ) {
        this.userRepository = userRepository;
        this.localCredentialRepository =
                localCredentialRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        return toResponse(findActiveUser(userId));
    }

    @Transactional
    public UserProfileResponse updateMyProfile(
            Long userId,
            UserProfileUpdateRequest request
    ) {
        String nickname = normalizeNickname(
                request.nickname()
        );

        if (nickname == null && request.gender() == null) {
            throw new RequestValidationException(
                    "request",
                    "수정할 필드를 하나 이상 입력해 주세요."
            );
        }

        User user = findActiveUser(userId);
        user.updateProfile(nickname, request.gender());

        try {
            userRepository.saveAndFlush(user);
        } catch (OptimisticLockingFailureException
                 | OptimisticLockException exception) {
            throw new BusinessException(
                    ErrorCode.USER_PROFILE_UPDATE_CONFLICT
            );
        }

        return toResponse(user);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ACCESS_TOKEN_INVALID
                        )
                );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_NOT_ACTIVE
            );
        }

        return user;
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null) {
            return null;
        }

        String normalized = nickname.trim();

        if (normalized.length() < 2
                || normalized.length() > 20) {
            throw new RequestValidationException(
                    "nickname",
                    "닉네임은 2~20자여야 합니다."
            );
        }

        if (normalized
                .codePoints()
                .anyMatch(Character::isISOControl)) {
            throw new RequestValidationException(
                    "nickname",
                    "닉네임에는 제어 문자를 사용할 수 없습니다."
            );
        }

        return normalized;
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                String.valueOf(user.getId()),
                user.getNickname(),
                user.getGender(),
                authenticationMethods(user.getId())
        );
    }

    private List<String> authenticationMethods(Long userId) {
        List<String> methods = new ArrayList<>();

        if (localCredentialRepository.existsByUser_Id(userId)) {
            methods.add("LOCAL");
        }

        socialAccountRepository.findProvidersByUserId(userId)
                .stream()
                .distinct()
                .map(Enum::name)
                .forEach(methods::add);

        return methods;
    }
}
