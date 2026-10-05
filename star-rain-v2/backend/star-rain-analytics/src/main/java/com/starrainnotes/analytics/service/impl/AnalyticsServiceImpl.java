package com.starrainnotes.analytics.service.impl;

import com.starrainnotes.analytics.api.ContentViewEvent;
import com.starrainnotes.analytics.dto.ReferrerClassification;
import com.starrainnotes.analytics.service.ReferrerClassifier;
import com.starrainnotes.analytics.entity.AnalyticsEventEntity;
import com.starrainnotes.analytics.exception.AnalyticsContentTypeInvalidException;
import com.starrainnotes.analytics.exception.AnalyticsDateRangeInvalidException;
import com.starrainnotes.analytics.exception.AnalyticsRouteInvalidException;
import com.starrainnotes.analytics.mapper.AnalyticsMapper;
import com.starrainnotes.analytics.service.AnalyticsService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
    private static final Set<String> PAGE_ROUTES = Set.of("/", "/blog", "/blog/archive", "/tutorials",
        "/portfolio", "/messages", "/about", "/search");
    private static final Set<String> CONTENT_TYPES = Set.of("BLOG", "TUTORIAL", "CHAPTER", "PORTFOLIO");
    private final AnalyticsMapper mapper;
    private final ReferrerClassifier classifier;

    @Override
    public void recordPageView(String routeKey, String referrer) {
        if (routeKey == null || !PAGE_ROUTES.contains(routeKey)) {
            throw new AnalyticsRouteInvalidException();
        }
        ReferrerClassification source = classifier.classify(referrer);
        AnalyticsEventEntity event = new AnalyticsEventEntity();
        event.setEventType("PAGE_VIEW");
        event.setRouteKey(routeKey);
        event.setReferrerCategory(source.getCategory());
        event.setReferrerHost(source.getHost());
        safelyInsert(event);
    }

    @Override
    public void recordContentView(ContentViewEvent event) {
        if (event == null || !CONTENT_TYPES.contains(event.getContentType())
            || event.getContentId() == null || event.getContentId() <= 0
            || event.getRouteKey() == null || !event.getRouteKey().matches("/[a-zA-Z0-9/_:-]{1,499}")) {
            throw new IllegalArgumentException("ANALYTICS_CONTENT_TYPE_INVALID");
        }
        AnalyticsEventEntity row = new AnalyticsEventEntity();
        row.setEventType("CONTENT_VIEW");
        row.setRouteKey(event.getRouteKey());
        row.setContentType(event.getContentType());
        row.setContentId(event.getContentId());
        row.setReferrerCategory("DIRECT");
        safelyInsert(row);
    }

    private void safelyInsert(AnalyticsEventEntity event) {
        try {
            mapper.insert(event);
        } catch (RuntimeException failure) {
            log.warn("Analytics event insert failed: type={}, route={}", event.getEventType(), event.getRouteKey(), failure);
        }
    }

    @Override
    public Map<String, Object> siteSummary() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<String, Object> history = mapper.historicalTotals(today);
        Map<String, Object> current = mapper.dayCounts(today);
        Map<String, Object> result = new HashMap<>();
        result.put("totalPageViews", count(history, "pageViews") + count(current, "pageViews"));
        result.put("totalContentViews", count(history, "contentViews") + count(current, "contentViews"));
        result.put("todayPageViews", count(current, "pageViews"));
        result.put("todayContentViews", count(current, "contentViews"));
        result.put("updatedAt", java.time.Instant.now().toString());
        return result;
    }

    @Override
    public List<Map<String, Object>> trend(LocalDate start, LocalDate end) {
        validateDates(start, end);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Map<String, Object>> result = new ArrayList<>(mapper.dailyTrend(start, end, today));
        if (!today.isBefore(start) && !today.isAfter(end)) {
            Map<String, Object> current = new HashMap<>(mapper.dayCounts(today));
            current.put("statDate", today);
            result.add(current);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> hotContent(String type, LocalDate start, LocalDate end, int limit) {
        validateDates(start, end);
        if ((type != null && !CONTENT_TYPES.contains(type)) || limit < 1 || limit > 100) {
            throw new AnalyticsContentTypeInvalidException();
        }
        return mapper.hotContent(start, end, LocalDate.now(ZoneOffset.UTC), type, limit);
    }

    @Override
    public List<Map<String, Object>> referrers(LocalDate start, LocalDate end) {
        validateDates(start, end);
        return mapper.referrers(start, end, LocalDate.now(ZoneOffset.UTC));
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)
            || java.time.temporal.ChronoUnit.DAYS.between(start, end) > 366
            || end.isAfter(LocalDate.now(ZoneOffset.UTC))) {
            throw new AnalyticsDateRangeInvalidException();
        }
    }

    private long count(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }
}
