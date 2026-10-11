package com.starrainnotes.tutorial.content.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.tutorial.content.dto.TutorialDashboardStatsDTO;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.service.impl.TutorialDashboardServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TutorialDashboardServiceImplTest {
    @Test
    void summaryUsesAggregateAndBoundedRecentQuery() {
        TutorialMapper mapper = mock(TutorialMapper.class);
        when(mapper.dashboardStats()).thenReturn(new TutorialDashboardStatsDTO(201, 7, 900, 23));
        TutorialEntity recent = new TutorialEntity();
        recent.setId(42L);
        recent.setTitle("Java 基础");
        recent.setPublicationStatus("PUBLISHED");
        recent.setEditingStatus("IN_REVIEW");
        recent.setUpdatedAt(LocalDateTime.of(2026, 10, 6, 12, 0));
        when(mapper.dashboardRecent()).thenReturn(List.of(recent));

        DashboardModuleData result = new TutorialDashboardServiceImpl(mapper).summary();

        assertEquals(201L, result.getMetrics().get("total"));
        assertEquals(23L, result.getMetrics().get("draftChapters"));
        assertEquals("42", result.getRecentContent().getFirst().getId());
        assertEquals("IN_REVIEW", result.getRecentContent().getFirst().getStatus());
        verify(mapper).dashboardStats();
        verify(mapper).dashboardRecent();
    }
}
