package com.starrainnotes.site.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardAggregationServiceTest {
    @Test
    void failedSourceDoesNotHideOtherModuleSummary() {
        DashboardSource blog = mock(DashboardSource.class);
        DashboardSource analytics = mock(DashboardSource.class);
        when(blog.moduleCode()).thenReturn("BLOG");
        when(blog.load()).thenReturn(new DashboardModuleData(Map.of("total", 5L), List.of()));
        when(analytics.moduleCode()).thenReturn("ANALYTICS");
        when(analytics.load()).thenThrow(new IllegalStateException("database unavailable"));
        DashboardAggregationService service = new DashboardAggregationService(List.of(blog, analytics));
        try {
            SiteDashboardView result = service.dashboard();
            assertEquals(5L, result.getModules().get("BLOG").getMetrics().get("total"));
            assertTrue(result.getDegradedModules().contains("ANALYTICS"));
        } finally {
            service.shutdown();
        }
    }
}
