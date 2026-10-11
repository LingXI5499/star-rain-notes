/*
 * SeoNotificationJob：公开页面与 SEO模块的定时任务文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 通过任务入口触发周期性处理，实际工作委托给本类依赖的服务；调度注解决定执行时间。
 * 主要协作类型：SeoNotificationService。
 * 阅读入口：run（运行SEO通知Job）。
 */

package com.starrainnotes.seo.job;

import com.starrainnotes.seo.service.SeoNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SEO通知Job，属于公开页面与 SEO模块。
 * 通过任务入口触发周期性处理，实际工作委托给本类依赖的服务；调度注解决定执行时间。
 */
@Component
@RequiredArgsConstructor
public class SeoNotificationJob {
    /**
     * notifications。
     * 协作类型：<code>SeoNotificationService</code>。
     */
    private final SeoNotificationService notifications;

    /**
     * 运行SEO通知Job。
     * 调用约束：<code>@Scheduled(fixedDelay = 60000, initialDelay = 60000)</code>。
     * 主要协作调用：<code>notifications.processPending</code>。
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void run() {
        notifications.processPending();
    }
}
