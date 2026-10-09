package com.starrainnotes.analytics.service;

import java.time.LocalDate;

public interface AnalyticsAggregateService {
    void aggregateDay(LocalDate date);

    void deleteRawBefore(LocalDate date);
}
