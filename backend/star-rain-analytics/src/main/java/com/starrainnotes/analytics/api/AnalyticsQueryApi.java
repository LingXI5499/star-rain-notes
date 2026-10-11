package com.starrainnotes.analytics.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AnalyticsQueryApi {
    Map<String, Object> siteSummary();
    List<Map<String, Object>> trend(LocalDate start, LocalDate end);
    List<Map<String, Object>> hotContent(String type, LocalDate start, LocalDate end, int limit);
    List<Map<String, Object>> referrers(LocalDate start, LocalDate end);
}
