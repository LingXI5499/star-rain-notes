package com.starrainnotes.common.dashboard.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardModuleData {
    private Map<String, Object> metrics;
    private List<DashboardRecentItem> recentContent;
}
