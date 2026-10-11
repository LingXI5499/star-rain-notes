/*
 * PortfolioDashboardSource：作品模块的模块支持文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：DashboardSource、DashboardModuleData、DashboardRecentItem、PageResult、PortfolioWorkService、WorkVO。
 * 阅读入口：moduleCode（module代码处理入口）；load（加载作品概览来源）。
 */

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

/**
 * 作品概览来源，属于作品模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class PortfolioDashboardSource implements DashboardSource {
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioWorkService</code>。
     */
    private final PortfolioWorkService works;

    /**
     * module代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String moduleCode() { return "PORTFOLIO"; }

    /**
     * 加载作品概览来源。
     * 主要协作调用：<code>works.adminWorks</code>。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
    @Override public DashboardModuleData load() {
        PageResult<WorkVO> recent = works.adminWorks(1, 8, null, null, null);
        long drafts = works.adminWorks(1, 1, null, "DRAFT", null).getTotal();
        List<DashboardRecentItem> items = recent.getItems().stream()
                .map(work -> new DashboardRecentItem(work.getId(), "PORTFOLIO",
                        work.getTitle(), work.getStatus(), work.getUpdatedAt())).toList();
        return new DashboardModuleData(Map.of("total", recent.getTotal(), "drafts", drafts), items);
    }
}
