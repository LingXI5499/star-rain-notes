package com.starrainnotes.tutorial.content.dashboard;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.tutorial.content.service.TutorialDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TutorialDashboardSource implements DashboardSource {
    private final TutorialDashboardService tutorials;

    @Override public String moduleCode() { return "TUTORIAL"; }

    @Override public DashboardModuleData load() {
        return tutorials.summary();
    }
}
