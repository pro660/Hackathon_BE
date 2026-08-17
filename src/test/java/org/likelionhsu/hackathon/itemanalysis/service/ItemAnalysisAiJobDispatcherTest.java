package org.likelionhsu.hackathon.itemanalysis.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.itemanalysis.service.ItemAnalysisAiJobProcessor.ProcessingResult;
import org.mockito.Mockito;
import org.springframework.core.task.AsyncTaskExecutor;

class ItemAnalysisAiJobDispatcherTest {

    @Test
    void dispatchExecutesProcessorWithImageInput() {
        ItemAnalysisAiJobProcessor processor =
                Mockito.mock(
                        ItemAnalysisAiJobProcessor.class
                );

        when(processor.process(
                1L,
                9101L,
                51L
        )).thenReturn(
                ProcessingResult.succeeded()
        );

        AsyncTaskExecutor taskExecutor =
                Mockito.mock(
                        AsyncTaskExecutor.class
                );

        Mockito.doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).execute(
                Mockito.any(Runnable.class)
        );

        ItemAnalysisAiJobDispatcher dispatcher =
                new ItemAnalysisAiJobDispatcher(
                        taskExecutor,
                        processor
                );

        dispatcher.dispatch(1L, 9101L, 51L);

        verify(processor).process(
                1L,
                9101L,
                51L
        );
    }
}
