/*
 * TutorialDashboardSource：教程与学习模块的模块支持文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：DashboardSource、DashboardModuleData、TutorialDashboardService。
 * 阅读入口：moduleCode（module代码处理入口）；load（加载教程概览来源）。
 */

package com.starrainnotes.tutorial.content.dashboard;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.tutorial.content.service.TutorialDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 教程概览来源，属于教程与学习模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class TutorialDashboardSource implements DashboardSource {
    /**
     * tutorials。
     * 协作类型：<code>TutorialDashboardService</code>。
     */
    private final TutorialDashboardService tutorials;

    /**
     * module代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String moduleCode() { return "TUTORIAL"; }

    /**
     * 加载教程概览来源。
     * 主要协作调用：<code>tutorials.summary</code>。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
    @Override public DashboardModuleData load() {
        return tutorials.summary();
    }
}
