package com.starrainnotes.analytics.dashboard;

import com.starrainnotes.analytics.api.AnalyticsQueryApi;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyticsDashboardSource implements DashboardSource {
    private final AnalyticsQueryApi analytics;

    @Override public String moduleCode() { return "ANALYTICS"; }

    @Override public DashboardModuleData load() {
        return new DashboardModuleData(analytics.siteSummary(), List.of());
    }
}
