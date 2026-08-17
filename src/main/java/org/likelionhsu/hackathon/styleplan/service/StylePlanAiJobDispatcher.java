package org.likelionhsu.hackathon.styleplan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

@Service
public class StylePlanAiJobDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    StylePlanAiJobDispatcher.class
            );

    private final AsyncTaskExecutor taskExecutor;
    private final StylePlanAiJobProcessor processor;

    public StylePlanAiJobDispatcher(
            @Qualifier("applicationTaskExecutor")
            AsyncTaskExecutor taskExecutor,
            StylePlanAiJobProcessor processor
    ) {
        this.taskExecutor = taskExecutor;
        this.processor = processor;
    }

    public void dispatch(
            Long userId,
            Long jobId,
            StylePlanJobRequest request
    ) {
        try {
            taskExecutor.execute(() ->
                    processSafely(
                            userId,
                            jobId,
                            request
                    )
            );
        } catch (TaskRejectedException exception) {
            log.error(
                    "STYLE_PLAN AI Job dispatch rejected. jobId={}",
                    jobId,
                    exception
            );
        }
    }

    private void processSafely(
            Long userId,
            Long jobId,
            StylePlanJobRequest request
    ) {
        try {
            processor.process(
                    userId,
                    jobId,
                    request
            );
        } catch (RuntimeException exception) {
            log.error(
                    "STYLE_PLAN AI Job processing failed. jobId={}",
                    jobId,
                    exception
            );
        }
    }
}
