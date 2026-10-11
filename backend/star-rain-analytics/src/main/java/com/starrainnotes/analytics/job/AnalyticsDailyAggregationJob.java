package com.starrainnotes.analytics.job;

import com.starrainnotes.analytics.service.AnalyticsAggregateService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyticsDailyAggregationJob {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsDailyAggregationJob.class);
    private final AnalyticsAggregateService aggregate;

    @Value("${star-rain.analytics.raw-event-retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "0 15 2 * * *", zone = "UTC")
    public void run() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int daysAgo = 3; daysAgo >= 1; daysAgo--) {
            LocalDate date = today.minusDays(daysAgo);
            try {
                aggregate.aggregateDay(date);
            } catch (RuntimeException failure) {
                log.error("Analytics aggregation failed for {}", date, failure);
                return;
            }
        }
        if (retentionDays > 3) {
            try {
                aggregate.deleteRawBefore(today.minusDays(retentionDays));
            } catch (RuntimeException failure) {
                log.error("Analytics raw retention failed", failure);
            }
        }
    }
}
