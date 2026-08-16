package org.likelionhsu.hackathon.purchaseutility.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;

class PurchaseUtilityAiJobDispatcherTest {

    @Test
    void dispatchSubmitsProcessingToTaskExecutor() {
        AsyncTaskExecutor taskExecutor =
                mock(AsyncTaskExecutor.class);
        PurchaseUtilityAiJobProcessor processor =
                mock(PurchaseUtilityAiJobProcessor.class);
        PurchaseUtilityAiJobDispatcher dispatcher =
                new PurchaseUtilityAiJobDispatcher(
                        taskExecutor,
                        processor
                );

        ArgumentCaptor<Runnable> taskCaptor =
                ArgumentCaptor.forClass(Runnable.class);

        dispatcher.dispatch(
                1L,
                900L,
                101L
        );

        verify(taskExecutor).execute(taskCaptor.capture());

        taskCaptor.getValue().run();

        verify(processor).process(
                1L,
                900L,
                101L,
                "ko"
        );
    }

    @Test
    void rejectedDispatchDoesNotFailCreateRequestThread() {
        AsyncTaskExecutor taskExecutor =
                mock(AsyncTaskExecutor.class);
        PurchaseUtilityAiJobProcessor processor =
                mock(PurchaseUtilityAiJobProcessor.class);
        PurchaseUtilityAiJobDispatcher dispatcher =
                new PurchaseUtilityAiJobDispatcher(
                        taskExecutor,
                        processor
                );

        doThrow(new TaskRejectedException("rejected"))
                .when(taskExecutor)
                .execute(any(Runnable.class));

        assertThatCode(() ->
                dispatcher.dispatch(
                        1L,
                        900L,
                        101L
                )
        ).doesNotThrowAnyException();
    }
}
