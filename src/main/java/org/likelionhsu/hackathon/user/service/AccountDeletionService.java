package org.likelionhsu.hackathon.user.service;

import java.time.Clock;
import java.time.Instant;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.auth.service.AccountReauthenticationService;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionService {

    private final AccountReauthenticationService
            accountReauthenticationService;
    private final UserRepository userRepository;
    private final UserDataDeletionService userDataDeletionService;
    private final Clock clock;

    public AccountDeletionService(
            AccountReauthenticationService
                    accountReauthenticationService,
            UserRepository userRepository,
            UserDataDeletionService userDataDeletionService,
            Clock clock
    ) {
        this.accountReauthenticationService =
                accountReauthenticationService;
        this.userRepository = userRepository;
        this.userDataDeletionService = userDataDeletionService;
        this.clock = clock;
    }

    @Transactional
    public void deleteAccount(
            Long userId,
            String reauthenticationToken
    ) {
        accountReauthenticationService.consume(
                userId,
                reauthenticationToken
        );

        User user = userRepository
                .findByIdForUpdate(userId)
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

        user.beginDeletion();
        userRepository.saveAndFlush(user);

        userDataDeletionService.deleteOwnedData(userId);

        user.completeDeletion(Instant.now(clock));
        userRepository.saveAndFlush(user);
    }
}
