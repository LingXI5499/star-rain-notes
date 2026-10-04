package com.starrainnotes.analytics.service;

import com.starrainnotes.analytics.api.AnalyticsQueryApi;
import com.starrainnotes.analytics.api.AnalyticsRecordApi;

public interface AnalyticsService extends AnalyticsRecordApi, AnalyticsQueryApi {
    void recordPageView(String routeKey, String referrer);
}
