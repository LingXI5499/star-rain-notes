/*
 * AnalyticsDailyAggregationJob：访问统计模块的定时任务文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 通过任务入口触发周期性处理，实际工作委托给本类依赖的服务；调度注解决定执行时间。
 * 主要协作类型：AnalyticsAggregateService。
 * 阅读入口：run（运行统计DailyAggregationJob）。
 */

package com.starrainnotes.analytics.job;

import com.starrainnotes.analytics.service.AnalyticsAggregateService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 统计DailyAggregationJob，属于访问统计模块。
 * 通过任务入口触发周期性处理，实际工作委托给本类依赖的服务；调度注解决定执行时间。
 */
@Component
@RequiredArgsConstructor
public class AnalyticsDailyAggregationJob {
    /**
     * 当前类使用的日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(AnalyticsDailyAggregationJob.class);
    /**
     * aggregate。
     * 协作类型：<code>AnalyticsAggregateService</code>。
     */
    private final AnalyticsAggregateService aggregate;

    /**
     * retentionDays。
     */
    @Value("${star-rain.analytics.raw-event-retention-days:90}")
    private int retentionDays;

    /**
     * 运行统计DailyAggregationJob。
     * 调用约束：<code>@Scheduled(cron = "0 15 2 * * *", zone = "UTC")</code>。
     * 主要协作调用：<code>aggregate.aggregateDay</code>、<code>aggregate.deleteRawBefore</code>。
     */
    @Scheduled(cron = "0 15 2 * * *", zone = "UTC")
    public void run() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int daysAgo = 3; daysAgo >= 1; daysAgo--) {
            LocalDate date = today.minusDays(daysAgo);
            try {
                aggregate.aggregateDay(date);
            } catch (RuntimeException failure) {
                log.error("Analytics aggregation failed for {}", date, failure);
                return;
            }
        }
        if (retentionDays > 3) {
            try {
                aggregate.deleteRawBefore(today.minusDays(retentionDays));
            } catch (RuntimeException failure) {
                log.error("Analytics raw retention failed", failure);
            }
        }
    }
}
