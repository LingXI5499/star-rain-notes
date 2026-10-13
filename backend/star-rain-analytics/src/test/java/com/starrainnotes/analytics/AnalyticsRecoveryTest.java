package com.starrainnotes.analytics;

import com.starrainnotes.analytics.job.AnalyticsDailyAggregationJob;
import com.starrainnotes.analytics.mapper.AnalyticsMapper;
import com.starrainnotes.analytics.service.AnalyticsAggregateService;
import com.starrainnotes.analytics.service.impl.AnalyticsAggregateServiceImpl;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

/** Recovery must finish all pending days before deleting any retained events. */
class AnalyticsRecoveryTest {
    /** A seven-day outage must include dates outside the former three-day window. */
    @Test
    void recoversSevenDayOutageBeforeRetention() {
        AnalyticsAggregateService aggregate = mock(AnalyticsAggregateService.class);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<LocalDate> dates = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(i -> today.minusDays(8 - i)).toList();
        when(aggregate.pendingDays(today)).thenReturn(dates);
        AnalyticsDailyAggregationJob job = new AnalyticsDailyAggregationJob(aggregate);
        ReflectionTestUtils.setField(job, "retentionDays", 90);
        job.run();
        var order = inOrder(aggregate);
        order.verify(aggregate).pendingDays(today);
        for (LocalDate date : dates) order.verify(aggregate).aggregateDay(date);
        order.verify(aggregate).deleteRawBefore(today.minusDays(90));
    }

    /** A failed day keeps the raw data available for the next complete pass. */
    @Test
    void failedDayPreventsRetention() {
        AnalyticsAggregateService aggregate = mock(AnalyticsAggregateService.class);
        LocalDate date = LocalDate.now(ZoneOffset.UTC).minusDays(7);
        when(aggregate.pendingDays(any())).thenReturn(List.of(date));
        doThrow(new IllegalStateException("isolated failure")).when(aggregate).aggregateDay(date);
        AnalyticsDailyAggregationJob job = new AnalyticsDailyAggregationJob(aggregate);
        ReflectionTestUtils.setField(job, "retentionDays", 90);
        job.run();
        verify(aggregate, never()).deleteRawBefore(any());
    }

    /** Rebuilding a day whose raw events have expired must not erase its summary. */
    @Test
    void preservesSummaryWithoutRawData() {
        AnalyticsMapper mapper = mock(AnalyticsMapper.class);
        new AnalyticsAggregateServiceImpl(mapper).aggregateDay(LocalDate.now(ZoneOffset.UTC).minusDays(91));
        verify(mapper, never()).deleteSiteDay(any());
        verify(mapper, never()).deleteContentDay(any());
        verify(mapper, never()).deleteReferrerDay(any());
    }
}
