package com.starrainnotes.tutorial.content.service.impl;

import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.common.dashboard.dto.DashboardRecentItem;
import com.starrainnotes.tutorial.content.dto.TutorialDashboardStatsDTO;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.service.TutorialDashboardService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorialDashboardServiceImpl implements TutorialDashboardService {
    private final TutorialMapper tutorials;

    @Override
    @Transactional(readOnly = true)
    public DashboardModuleData summary() {
        TutorialDashboardStatsDTO stats = tutorials.dashboardStats();
        return new DashboardModuleData(Map.of(
            "total", stats.getTotal(), "drafts", stats.getDrafts(),
            "chapters", stats.getChapters(), "draftChapters", stats.getDraftChapters()),
            tutorials.dashboardRecent().stream()
                .map(row -> new DashboardRecentItem(String.valueOf(row.getId()), "TUTORIAL", row.getTitle(),
                    "IN_REVIEW".equals(row.getEditingStatus()) ? "IN_REVIEW" : row.getPublicationStatus(),
                    row.getUpdatedAt()))
                .toList());
    }
}
