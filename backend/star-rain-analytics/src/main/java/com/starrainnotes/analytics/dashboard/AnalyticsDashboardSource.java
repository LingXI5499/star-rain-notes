/*
 * AnalyticsDashboardSource：访问统计模块的模块支持文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：AnalyticsQueryApi、DashboardSource、DashboardModuleData。
 * 阅读入口：moduleCode（module代码处理入口）；load（加载统计概览来源）。
 */

package com.starrainnotes.analytics.dashboard;

import com.starrainnotes.analytics.api.AnalyticsQueryApi;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 统计概览来源，属于访问统计模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class AnalyticsDashboardSource implements DashboardSource {
    /**
     * 统计。
     * 协作类型：<code>AnalyticsQueryApi</code>。
     */
    private final AnalyticsQueryApi analytics;

    /**
     * module代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String moduleCode() { return "ANALYTICS"; }

    /**
     * 加载统计概览来源。
     * 主要协作调用：<code>analytics.siteSummary</code>。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
    @Override public DashboardModuleData load() {
        return new DashboardModuleData(analytics.siteSummary(), List.of());
    }
}
