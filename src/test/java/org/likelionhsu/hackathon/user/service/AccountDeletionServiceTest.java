package org.likelionhsu.hackathon.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.auth.service.AccountReauthenticationService;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant NOW =
            Instant.parse("2026-08-16T05:00:00Z");

    @Mock
    private AccountReauthenticationService
            accountReauthenticationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDataDeletionService userDataDeletionService;

    private AccountDeletionService service;

    @BeforeEach
    void setUp() {
        service = new AccountDeletionService(
                accountReauthenticationService,
                userRepository,
                userDataDeletionService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void verifiedAccountIsDeletedAndAnonymized() {
        User user = User.local(
                "user@example.com",
                "사용자",
                Gender.MALE
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(userRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(user));

        service.deleteAccount(USER_ID, "raw-token");

        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getEmail())
                .isEqualTo("deleted-1@invalid.local");
        assertThat(user.getNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(user.getGender())
                .isEqualTo(Gender.NOT_SPECIFIED);

        InOrder order = inOrder(
                accountReauthenticationService,
                userRepository,
                userDataDeletionService
        );
        order.verify(accountReauthenticationService)
                .consume(USER_ID, "raw-token");
        order.verify(userRepository).findByIdForUpdate(USER_ID);
        order.verify(userRepository).saveAndFlush(user);
        order.verify(userDataDeletionService)
                .deleteOwnedData(USER_ID);
        order.verify(userRepository).saveAndFlush(user);
    }
}
