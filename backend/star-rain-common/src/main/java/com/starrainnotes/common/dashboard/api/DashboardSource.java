package com.starrainnotes.common.dashboard.api;

import com.starrainnotes.common.dashboard.dto.DashboardModuleData;

public interface DashboardSource {
    String moduleCode();
    DashboardModuleData load();
}
