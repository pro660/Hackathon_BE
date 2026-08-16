package org.likelionhsu.hackathon.purchaseutility.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

@Service
public class PurchaseUtilityAiJobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(
            PurchaseUtilityAiJobDispatcher.class
    );

    private final AsyncTaskExecutor taskExecutor;
    private final PurchaseUtilityAiJobProcessor processor;

    public PurchaseUtilityAiJobDispatcher(
            @Qualifier("applicationTaskExecutor")
            AsyncTaskExecutor taskExecutor,
            PurchaseUtilityAiJobProcessor processor
    ) {
        this.taskExecutor = taskExecutor;
        this.processor = processor;
    }

    public void dispatch(
            Long userId,
            Long jobId,
            Long productId
    ) {
        try {
            taskExecutor.execute(() ->
                    processSafely(
                            userId,
                            jobId,
                            productId
                    )
            );
        } catch (TaskRejectedException exception) {
            log.error(
                    "Purchase Utility AI Job dispatch rejected. jobId={}",
                    jobId,
                    exception
            );
        }
    }

    private void processSafely(
            Long userId,
            Long jobId,
            Long productId
    ) {
        try {
            processor.process(
                    userId,
                    jobId,
                    productId,
                    "ko"
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Purchase Utility AI Job processing failed. jobId={}",
                    jobId,
                    exception
            );
        }
    }
}
