package com.starrainnotes.seo.job;

import com.starrainnotes.seo.service.SeoNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeoNotificationJob {
    private final SeoNotificationService notifications;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void run() {
        notifications.processPending();
    }
}
