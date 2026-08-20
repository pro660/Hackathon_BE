package org.likelionhsu.hackathon.auth.service;

import org.likelionhsu.hackathon.auth.domain.LocalCredential;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.dto.request.PasswordChangeRequest;
import org.likelionhsu.hackathon.auth.repository.LocalCredentialRepository;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordChangeService(
            UserRepository userRepository,
            LocalCredentialRepository localCredentialRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.localCredentialRepository = localCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(
            Long userId,
            PasswordChangeRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ACCESS_TOKEN_INVALID
                        )
                );
        ensureActive(user);

        LocalCredential credential = localCredentialRepository
                .findWithUserByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode
                                        .PASSWORD_CHANGE_NOT_AVAILABLE
                        )
                );

        if (!passwordEncoder.matches(
                request.currentPassword(),
                credential.getPasswordHash()
        )) {
            throw new BusinessException(
                    ErrorCode.CURRENT_PASSWORD_MISMATCH
            );
        }

        if (!request.newPassword().equals(
                request.newPasswordConfirm()
        )) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_CONFIRM_MISMATCH
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                credential.getPasswordHash()
        )) {
            throw new BusinessException(
                    ErrorCode.NEW_PASSWORD_SAME_AS_CURRENT
            );
        }

        credential.changePassword(
                passwordEncoder.encode(request.newPassword())
        );
        localCredentialRepository.saveAndFlush(credential);
    }

    private void ensureActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }
}
