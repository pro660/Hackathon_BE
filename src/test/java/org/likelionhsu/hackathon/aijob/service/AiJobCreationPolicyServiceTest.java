package org.likelionhsu.hackathon.aijob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.aijob.repository.AiJobCreationPolicyRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiJobCreationPolicyServiceTest {

    @Mock
    private AiJobCreationPolicyRepository repository;

    private AiJobCreationPolicyService service;

    @BeforeEach
    void setUp() {
        service = new AiJobCreationPolicyService(
                repository,
                10
        );
    }

    @Test
    void creationRunsAfterUserLockAndPolicyChecks() {
        when(repository
                .existsRunningJobExceptIdempotencyKey(
                        1L,
                        "new-key"
                )
        ).thenReturn(false);
        when(repository
                .countCreatedInLastTwentyFourHoursExceptIdempotencyKey(
                        1L,
                        "new-key"
                )
        ).thenReturn(9);

        String result = service.execute(
                1L,
                "new-key",
                () -> "created"
        );

        assertThat(result).isEqualTo("created");
        verify(repository).lockUser(1L);
        verify(repository).expireStaleRunningJobs(1L);
    }

    @Test
    void anotherRunningJobIsRejected() {
        when(repository
                .existsRunningJobExceptIdempotencyKey(
                        1L,
                        "new-key"
                )
        ).thenReturn(true);

        AtomicBoolean created =
                new AtomicBoolean(false);

        assertThatThrownBy(() ->
                service.execute(
                        1L,
                        "new-key",
                        () -> {
                            created.set(true);
                            return "created";
                        }
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.AI_JOB_ALREADY_RUNNING
                        )
                );

        assertThat(created).isFalse();
        verify(repository, never())
                .countCreatedInLastTwentyFourHoursExceptIdempotencyKey(
                        1L,
                        "new-key"
                );
    }

    @Test
    void tenthRecentJobBlocksEleventhRequest() {
        when(repository
                .existsRunningJobExceptIdempotencyKey(
                        1L,
                        "new-key"
                )
        ).thenReturn(false);
        when(repository
                .countCreatedInLastTwentyFourHoursExceptIdempotencyKey(
                        1L,
                        "new-key"
                )
        ).thenReturn(10);

        assertThatThrownBy(() ->
                service.execute(
                        1L,
                        "new-key",
                        () -> "created"
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(
                                ((BusinessException) exception)
                                        .getErrorCode()
                        ).isEqualTo(
                                ErrorCode.AI_DAILY_LIMIT_EXCEEDED
                        )
                );
    }
}
