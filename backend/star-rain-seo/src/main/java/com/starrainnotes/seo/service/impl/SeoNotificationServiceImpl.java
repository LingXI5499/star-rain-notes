/*
 * SeoNotificationServiceImpl：公开页面与 SEO模块的业务实现文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：NotificationResult、SeoChange、SeoNotificationLog、IndexNowNotifier、SeoNotificationMapper、SeoNotificationService、CanonicalUrlResolver。
 * 阅读入口：enqueue（enqueue处理入口）；processPending（处理待处理）。
 */

package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.dto.NotificationResult;
import com.starrainnotes.seo.dto.SeoChange;
import com.starrainnotes.seo.entity.SeoNotificationLog;
import com.starrainnotes.seo.notifier.IndexNowNotifier;
import com.starrainnotes.seo.mapper.SeoNotificationMapper;
import com.starrainnotes.seo.service.SeoNotificationService;
import com.starrainnotes.seo.config.CanonicalUrlResolver;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * SEO通知业务服务实现，属于公开页面与 SEO模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class SeoNotificationServiceImpl implements SeoNotificationService {
    /**
     * 当前类使用的日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(SeoNotificationServiceImpl.class);
    /**
     * 数据映射。
     * 协作类型：<code>SeoNotificationMapper</code>。
     */
    private final SeoNotificationMapper mapper;
    /**
     * notifier。
     */
    private final IndexNowNotifier notifier;
    /**
     * 规范。
     */
    private final CanonicalUrlResolver canonical;

    /**
     * enqueue处理入口。
     * 事务声明：<code>@Transactional(propagation = Propagation.REQUIRES_NEW)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>canonical.canonical</code>、<code>notifier.enabled</code>、<code>notifier.providerCode</code>、<code>mapper.enqueue</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>；具体触发条件见对应分支。
     *
     * @param routePath 路由路径；类型为 <code>String</code>
     * @param changeType change类型；类型为 <code>String</code>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(String routePath, String changeType) {
        String url = canonical.canonical(routePath);
        if (!("UPSERT".equals(changeType) || "DELETE".equals(changeType))) {
            throw new IllegalArgumentException("Invalid SEO change type");
        }
        if (!notifier.enabled()) return;
        SeoNotificationLog row = new SeoNotificationLog();
        row.setProviderCode(notifier.providerCode());
        row.setRoutePath(routePath);
        row.setCanonicalUrl(url);
        row.setChangeType(changeType);
        mapper.enqueue(row);
    }

    /**
     * 处理待处理。
     * 主要协作调用：<code>mapper.due</code>、<code>notifier.enabled</code>、<code>notifier.providerCode</code>、<code>mapper.claim</code>、<code>notifier.notify</code>。
     */
    @Override
    public void processPending() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (SeoNotificationLog row : mapper.due(now, 50)) {
            if (!notifier.enabled() || !notifier.providerCode().equals(row.getProviderCode())
                    || mapper.claim(row.getId(), now) == 0) continue;
            NotificationResult result;
            try {
                result = notifier.notify(SeoChange.builder().routePath(row.getRoutePath())
                    .canonicalUrl(row.getCanonicalUrl()).changeType(row.getChangeType()).build());
            } catch (RuntimeException exception) {
                result = NotificationResult.builder().errorCode("PROVIDER_ERROR").build();
            }
            if (result.isSuccess()) mapper.success(row.getId(), result.getHttpStatus());
            else {
                int minutes = Math.min(60, 1 << Math.min(6, row.getAttemptCount() + 1));
                mapper.failure(row.getId(), result.getHttpStatus(),
                    result.getErrorCode() == null ? "NOTIFICATION_FAILED" : result.getErrorCode(),
                    now.plusMinutes(minutes));
                log.warn("SEO notification failed for provider {} and route {}; retry scheduled",
                    row.getProviderCode(), row.getRoutePath());
            }
        }
    }
}
