/*
 * TutorialDashboardServiceImplTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：DashboardModuleData、TutorialDashboardStatsDTO、TutorialEntity、TutorialMapper、TutorialDashboardServiceImpl。
 * 阅读入口：summaryUsesAggregateAndBoundedRecentQuery（摘要UsesAggregate及BoundedRecent查询参数处理入口）。
 */

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

/**
 * 教程概览业务服务实现测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class TutorialDashboardServiceImplTest {
    /**
     * 摘要UsesAggregate及BoundedRecent查询参数处理入口。
     * 关键处理步骤调用 <code>mapper.dashboardStats</code>、<code>recent.setId</code>、<code>recent.setTitle</code>、<code>recent.setPublicationStatus</code>。
     */
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
