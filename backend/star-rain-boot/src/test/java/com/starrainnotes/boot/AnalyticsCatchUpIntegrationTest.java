package com.starrainnotes.boot;

import com.starrainnotes.analytics.mapper.AnalyticsMapper;
import com.starrainnotes.analytics.service.impl.AnalyticsAggregateServiceImpl;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.sql.Timestamp;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Real MySQL recovery and retention checks; every fixture rolls back in the isolated test DB. */
class AnalyticsCatchUpIntegrationTest extends MapperXmlIntegrationSupport {
    /** Seven missing UTC days, rebuilt three times, remain consistent across every query. */
    @Test void sevenDayOutageIsIdempotentAcrossAllQueries() throws Exception {
        try (SqlSession session = openSession(); AutoCloseable rollback = () -> session.rollback(true)) {
            AnalyticsMapper mapper = session.getMapper(AnalyticsMapper.class);
            AnalyticsAggregateServiceImpl service = new AnalyticsAggregateServiceImpl(mapper);
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            long before = ((Number) mapper.historicalTotals(today).get("pageViews")).longValue();
            for (int day = 7; day >= 1; day--) {
                event(session, today.minusDays(day), "PAGE_VIEW");
                event(session, today.minusDays(day), "CONTENT_VIEW");
            }
            assertThat(service.pendingDays(today)).contains(today.minusDays(7));
            for (int pass = 0; pass < 3; pass++) {
                for (LocalDate day : service.pendingDays(today)) service.aggregateDay(day);
            }
            assertThat(((Number) mapper.historicalTotals(today).get("pageViews")).longValue()).isEqualTo(before + 7);
            assertThat(mapper.dailyTrend(today.minusDays(7), today.minusDays(1), today)).hasSize(7);
            var hot = mapper.hotContent(today.minusDays(7), today.minusDays(1), today, "BLOG", 50);
            assertThat(hot.stream().filter(row -> ((Number) row.get("contentId")).longValue() == 987654321L)
                    .mapToLong(row -> ((Number) row.get("viewCount")).longValue()).sum()).isEqualTo(7);
            assertThat(mapper.referrers(today.minusDays(7), today.minusDays(1), today)
                    .stream().mapToLong(row -> ((Number) row.get("viewCount")).longValue()).sum()).isEqualTo(7);
            session.rollback();
        }
    }

    /** Catch up dates before the cutoff before pruning, and never erase expired summaries. */
    @Test void preservesNinetyDayBoundaryAndExpiredSummaries() throws Exception {
        try (SqlSession session = openSession(); AutoCloseable rollback = () -> session.rollback(true)) {
            AnalyticsMapper mapper = session.getMapper(AnalyticsMapper.class);
            AnalyticsAggregateServiceImpl service = new AnalyticsAggregateServiceImpl(mapper);
            LocalDate today = LocalDate.now(ZoneOffset.UTC), expired = today.minusDays(91), boundary = today.minusDays(90);
            event(session, expired, "PAGE_VIEW"); event(session, boundary, "PAGE_VIEW"); event(session, today, "PAGE_VIEW");
            assertThat(service.pendingDays(today)).contains(expired, boundary).doesNotContain(today);
            service.aggregateDay(expired); service.aggregateDay(boundary);
            service.deleteRawBefore(boundary);
            assertThat(mapper.rawDayCount(expired)).isZero(); assertThat(mapper.rawDayCount(boundary)).isEqualTo(1);
            service.aggregateDay(expired);
            assertThat(mapper.dailyTrend(expired, expired, today)).hasSize(1);
            session.rollback();
        }
    }

    /** Insert only controlled analytics fixtures with an explicit UTC midnight. */
    private void event(SqlSession session, LocalDate date, String type) throws Exception {
        try (var statement = session.getConnection().prepareStatement(
                "INSERT INTO sr_analytics_event(event_type,route_key,content_type,content_id,referrer_category,occurred_at) VALUES (?, '/v21-test', 'BLOG', 987654321, 'DIRECT', ?)")) {
            statement.setString(1, type);
            statement.setTimestamp(2, Timestamp.from(date.atStartOfDay(ZoneOffset.UTC).toInstant()),
                    java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")));
            statement.executeUpdate();
        }
    }
}
