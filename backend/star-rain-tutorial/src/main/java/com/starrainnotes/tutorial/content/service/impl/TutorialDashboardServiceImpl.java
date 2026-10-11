/*
 * TutorialDashboardServiceImpl：教程与学习模块的业务实现文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：DashboardModuleData、DashboardRecentItem、TutorialDashboardStatsDTO、TutorialMapper、TutorialDashboardService。
 * 阅读入口：summary（摘要处理入口）。
 */

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

/**
 * 教程概览业务服务实现，属于教程与学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class TutorialDashboardServiceImpl implements TutorialDashboardService {
    /**
     * tutorials。
     * 协作类型：<code>TutorialMapper</code>。
     */
    private final TutorialMapper tutorials;

    /**
     * 摘要处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tutorials.dashboardStats</code>、<code>tutorials.dashboardRecent</code>。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
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
