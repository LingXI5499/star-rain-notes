package com.starrainnotes.site.vo;

import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.common.dashboard.dto.DashboardRecentItem;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SiteDashboardVO {
    private Map<String, DashboardModuleData> modules;
    private List<DashboardRecentItem> recentContent;
    private List<String> degradedModules;
}
