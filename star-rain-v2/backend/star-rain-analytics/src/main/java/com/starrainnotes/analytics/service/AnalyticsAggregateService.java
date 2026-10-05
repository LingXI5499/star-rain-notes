package com.starrainnotes.analytics.service;

import com.starrainnotes.analytics.mapper.AnalyticsMapper;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsAggregateService {
    private final AnalyticsMapper mapper;

    @Transactional
    public void aggregateDay(LocalDate date) {
        if (date == null || !date.isBefore(LocalDate.now(java.time.ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Only completed UTC days can be aggregated");
        }
        mapper.deleteSiteDay(date);
        mapper.deleteContentDay(date);
        mapper.deleteReferrerDay(date);
        mapper.insertSiteDay(date);
        mapper.insertContentDay(date);
        mapper.insertReferrerDay(date);
    }
}
