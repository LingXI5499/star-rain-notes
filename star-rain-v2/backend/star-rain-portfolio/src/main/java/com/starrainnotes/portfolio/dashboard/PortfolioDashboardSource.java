package com.starrainnotes.portfolio.dashboard;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.common.dashboard.dto.DashboardRecentItem;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.portfolio.service.PortfolioWorkService;
import com.starrainnotes.portfolio.vo.WorkVO;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioDashboardSource implements DashboardSource {
    private final PortfolioWorkService works;

    @Override public String moduleCode() { return "PORTFOLIO"; }

    @Override public DashboardModuleData load() {
        PageResult<WorkVO> recent = works.adminWorks(1, 8, null, null, null);
        long drafts = works.adminWorks(1, 1, null, "DRAFT", null).getTotal();
        List<DashboardRecentItem> items = recent.getItems().stream()
                .map(work -> new DashboardRecentItem(work.getId(), "PORTFOLIO",
                        work.getTitle(), work.getStatus(), work.getUpdatedAt())).toList();
        return new DashboardModuleData(Map.of("total", recent.getTotal(), "drafts", drafts), items);
    }
}
