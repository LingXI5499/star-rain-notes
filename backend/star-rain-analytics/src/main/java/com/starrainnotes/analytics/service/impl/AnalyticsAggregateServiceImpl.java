/*
 * AnalyticsAggregateServiceImpl：访问统计模块的业务实现文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：AnalyticsMapper、AnalyticsAggregateService。
 * 阅读入口：aggregateDay（aggregate日处理入口）；deleteRawBefore（删除原始之前）。
 */

package com.starrainnotes.analytics.service.impl;

import com.starrainnotes.analytics.mapper.AnalyticsMapper;
import com.starrainnotes.analytics.service.AnalyticsAggregateService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 统计Aggregate业务服务实现，属于访问统计模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class AnalyticsAggregateServiceImpl implements AnalyticsAggregateService {
    /**
     * 数据映射。
     * 协作类型：<code>AnalyticsMapper</code>。
     */
    private final AnalyticsMapper mapper;

    /** Enumerate retained dates; no checkpoint can hide an older missing day. */
    @Override
    public List<LocalDate> pendingDays(LocalDate today) {
        return mapper.pendingDays(today);
    }

    /**
     * aggregate日处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.deleteSiteDay</code>、<code>mapper.deleteContentDay</code>、<code>mapper.deleteReferrerDay</code>、<code>mapper.insertSiteDay</code>、<code>mapper.insertContentDay</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param date 日期；类型为 <code>LocalDate</code>
     */
    @Override
    @Transactional
    public void aggregateDay(LocalDate date) {
        if (date == null || !date.isBefore(LocalDate.now(java.time.ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Only completed UTC days can be aggregated");
        }
        // Expired raw data cannot reconstruct an existing valuable summary.
        if (mapper.rawDayCount(date) == 0) return;
        mapper.deleteSiteDay(date);
        mapper.deleteContentDay(date);
        mapper.deleteReferrerDay(date);
        mapper.insertSiteDay(date);
        mapper.insertContentDay(date);
        mapper.insertReferrerDay(date);
    }

    /**
     * 删除原始之前。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.deleteRawBefore</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param date 日期；类型为 <code>LocalDate</code>
     */
    @Override
    public void deleteRawBefore(LocalDate date) {
        if (date == null || !date.isBefore(LocalDate.now(java.time.ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Raw retention cutoff must be before today UTC");
        }
        // Each bounded delete commits independently; the next scheduled run continues any backlog.
        for (int batch = 0; batch < 100; batch++) {
            if (mapper.deleteRawBefore(date) < 1000) break;
        }
    }
}
