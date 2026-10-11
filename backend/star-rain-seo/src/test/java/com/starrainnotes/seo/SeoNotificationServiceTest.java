/*
 * SeoNotificationServiceTest：公开页面与 SEO模块的测试文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CanonicalUrlResolver、SeoNotificationMapper、NotificationResult、IndexNowNotifier、SeoChange、SeoNotificationLog、SeoNotificationServiceImpl。
 * 阅读入口：failedProviderIsRetriedWithoutRecordingASecret（失败提供方是否Retried不含RecordingASecret处理入口）。
 */

package com.starrainnotes.seo;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.seo.config.CanonicalUrlResolver;
import com.starrainnotes.seo.mapper.SeoNotificationMapper;
import com.starrainnotes.seo.dto.NotificationResult;
import com.starrainnotes.seo.notifier.IndexNowNotifier;
import com.starrainnotes.seo.dto.SeoChange;
import com.starrainnotes.seo.entity.SeoNotificationLog;
import com.starrainnotes.seo.service.impl.SeoNotificationServiceImpl;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * SEO通知业务服务测试，属于公开页面与 SEO模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class SeoNotificationServiceTest {
    /**
     * 失败提供方是否Retried不含RecordingASecret处理入口。
     * 关键处理步骤调用 <code>notifier.enabled</code>、<code>notifier.providerCode</code>、<code>row.setId</code>、<code>row.setProviderCode</code>。
     */
    @Test
    void failedProviderIsRetriedWithoutRecordingASecret() {
        SeoNotificationMapper mapper = mock(SeoNotificationMapper.class);
        IndexNowNotifier notifier = mock(IndexNowNotifier.class);
        CanonicalUrlResolver canonical = mock(CanonicalUrlResolver.class);
        when(notifier.enabled()).thenReturn(true);
        when(notifier.providerCode()).thenReturn("TEST");
        SeoNotificationLog row = new SeoNotificationLog();
        row.setId(7L);
        row.setProviderCode("TEST");
        row.setRoutePath("/blog/posts/test");
        row.setCanonicalUrl("https://example.org/blog/posts/test");
        row.setChangeType("UPSERT");
        row.setAttemptCount(0);
        when(mapper.due(any(), eq(50))).thenReturn(List.of(row));
        when(mapper.claim(eq(7L), any())).thenReturn(1);
        when(notifier.notify(any(SeoChange.class))).thenReturn(
            NotificationResult.builder().httpStatus(503).errorCode("HTTP_503").build());

        new SeoNotificationServiceImpl(mapper, notifier, canonical).processPending();

        ArgumentCaptor<LocalDateTime> next = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).failure(eq(7L), eq(503), eq("HTTP_503"), next.capture());
        assertTrue(next.getValue().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
    }
}
