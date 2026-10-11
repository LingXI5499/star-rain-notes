/*
 * ReviewDashboardSource：审核模块的模块支持文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：DashboardSource、DashboardModuleData、ReviewQueryDTO、ReviewQueryService。
 * 阅读入口：moduleCode（module代码处理入口）；load（加载审核概览来源）。
 */

package com.starrainnotes.review.dashboard;

import com.starrainnotes.common.dashboard.api.DashboardSource;
import com.starrainnotes.common.dashboard.dto.DashboardModuleData;
import com.starrainnotes.review.dto.ReviewQueryDTO;
import com.starrainnotes.review.service.ReviewQueryService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 审核概览来源，属于审核模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class ReviewDashboardSource implements DashboardSource {
    /**
     * reviews。
     * 协作类型：<code>ReviewQueryService</code>。
     */
    private final ReviewQueryService reviews;

    /**
     * module代码处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override public String moduleCode() { return "REVIEW"; }

    /**
     * 加载审核概览来源。
     * 主要协作调用：<code>reviews.pagePending</code>。
     * @return 类型为 <code>DashboardModuleData</code> 的处理结果
     */
    @Override public DashboardModuleData load() {
        ReviewQueryDTO query = new ReviewQueryDTO();
        query.setPageSize(1);
        return new DashboardModuleData(Map.of("pending", reviews.pagePending(query).getTotal()), List.of());
    }
}
