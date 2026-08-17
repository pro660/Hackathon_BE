package org.likelionhsu.hackathon.itemanalysis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

@Service
public class ItemAnalysisAiJobDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ItemAnalysisAiJobDispatcher.class
            );

    private final AsyncTaskExecutor taskExecutor;
    private final ItemAnalysisAiJobProcessor processor;

    public ItemAnalysisAiJobDispatcher(
            @Qualifier("applicationTaskExecutor")
            AsyncTaskExecutor taskExecutor,
            ItemAnalysisAiJobProcessor processor
    ) {
        this.taskExecutor = taskExecutor;
        this.processor = processor;
    }

    public void dispatch(
            Long userId,
            Long jobId,
            Long imageAssetId
    ) {
        try {
            taskExecutor.execute(() ->
                    processSafely(
                            userId,
                            jobId,
                            imageAssetId
                    )
            );
        } catch (TaskRejectedException exception) {
            log.error(
                    "ITEM_ANALYSIS AI Job dispatch rejected. jobId={}",
                    jobId,
                    exception
            );
        }
    }

    private void processSafely(
            Long userId,
            Long jobId,
            Long imageAssetId
    ) {
        try {
            processor.process(
                    userId,
                    jobId,
                    imageAssetId
            );
        } catch (RuntimeException exception) {
            log.error(
                    "ITEM_ANALYSIS AI Job processing failed. jobId={}",
                    jobId,
                    exception
            );
        }
    }
}
