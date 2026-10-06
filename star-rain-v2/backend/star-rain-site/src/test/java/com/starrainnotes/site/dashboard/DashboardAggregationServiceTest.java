package com.starrainnotes.site.dashboard;

import com.starrainnotes.site.vo.SiteDashboardVO;
import com.starrainnotes.site.service.impl.DashboardAggregationServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        DashboardAggregationServiceImpl service = new DashboardAggregationServiceImpl(List.of(blog, analytics));
        try {
            SiteDashboardVO result = service.dashboard();
            assertEquals(5L, result.getModules().get("BLOG").getMetrics().get("total"));
            assertTrue(result.getDegradedModules().contains("ANALYTICS"));
        } finally {
            service.shutdown();
        }
    }

    @Test
    void duplicateModuleCodeFailsAtStartup() {
        DashboardSource first = mock(DashboardSource.class);
        DashboardSource second = mock(DashboardSource.class);
        when(first.moduleCode()).thenReturn("BLOG");
        when(second.moduleCode()).thenReturn("BLOG");
        assertThrows(IllegalArgumentException.class,
            () -> new DashboardAggregationServiceImpl(List.of(first, second)));
    }

    @Test
    void incompleteSourceDegradesWithoutDroppingOtherModules() {
        DashboardSource broken = mock(DashboardSource.class);
        DashboardSource healthy = mock(DashboardSource.class);
        when(broken.moduleCode()).thenReturn("BLOG");
        when(broken.load()).thenReturn(null);
        when(healthy.moduleCode()).thenReturn("ANALYTICS");
        when(healthy.load()).thenReturn(new DashboardModuleData(Map.of("views", 3L), List.of()));
        DashboardAggregationServiceImpl service = new DashboardAggregationServiceImpl(List.of(broken, healthy));
        try {
            SiteDashboardVO result = service.dashboard();
            assertTrue(result.getDegradedModules().contains("BLOG"));
            assertEquals(3L, result.getModules().get("ANALYTICS").getMetrics().get("views"));
        } finally {
            service.shutdown();
        }
    }
}
