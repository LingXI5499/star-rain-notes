package com.starrainnotes.analytics.service.impl;

import com.starrainnotes.analytics.mapper.AnalyticsMapper;
import com.starrainnotes.analytics.service.AnalyticsAggregateService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsAggregateServiceImpl implements AnalyticsAggregateService {
    private final AnalyticsMapper mapper;

    @Override
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

    @Override
    @Transactional
    public void deleteRawBefore(LocalDate date) {
        if (date == null || !date.isBefore(LocalDate.now(java.time.ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Raw retention cutoff must be before today UTC");
        }
        mapper.deleteRawBefore(date);
    }
}
