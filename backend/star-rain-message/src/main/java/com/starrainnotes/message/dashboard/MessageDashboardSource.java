/*
 * MessageDashboardSource：留言模块的模块支持文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：MessageSummaryApi、DashboardSource、DashboardModuleData。
 * 阅读入口：moduleCode（module代码处理入口）；load（加载留言概览来源）。
 */

package com.starrainnotes.message.dashboard;

import com.starrainnotes.message.api.MessageSummaryApi;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 留言概览来源，属于留言模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class MessageDashboardSource implements DashboardSource {
    /**
     * messages。
     * 协作类型：<code>MessageSummaryApi</code>。
     */
    private final MessageSummaryApi messages;

    /**
     * module代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String moduleCode() { return "MESSAGE"; }

    /**
     * 加载留言概览来源。
     * 主要协作调用：<code>messages.pendingCount</code>。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
    @Override public DashboardModuleData load() {
        return new DashboardModuleData(Map.of("pending", messages.pendingCount()), List.of());
    }
}
