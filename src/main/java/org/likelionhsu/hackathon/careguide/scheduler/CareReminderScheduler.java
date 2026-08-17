package org.likelionhsu.hackathon.careguide.scheduler;

import org.likelionhsu.hackathon.careguide.service.CareReminderGenerationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.care-reminders.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class CareReminderScheduler {

    private final CareReminderGenerationService generationService;

    public CareReminderScheduler(
            CareReminderGenerationService generationService
    ) {
        this.generationService = generationService;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.care-reminders.scheduler.fixed-delay:1h}",
            initialDelayString =
                    "${app.care-reminders.scheduler.initial-delay:1m}"
    )
    public void generateReminders() {
        generationService.generateForToday();
    }
}
