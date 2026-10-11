/*
 * AnalyticsServiceTest：访问统计模块的测试文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：AnalyticsAggregateServiceImpl、ContentViewEvent、ReferrerClassifier、AnalyticsEventEntity、AnalyticsDateRangeInvalidException、AnalyticsRouteInvalidException、AnalyticsMapper。
 * 阅读入口：pageViewOnlyAcceptsKnownPublicRoutes（分页读取ViewOnlyAcceptsKnown公开路由集合）；configuredSiteHostIsInternal（configured站点主机是否Internal处理入口）；analyticsInsertFailureDoesNotFailPublicContent（统计新增失败Does不Fail公开内容处理入口）；invalidContentAndDateRangeAreRejected（无效内容及日期范围Are已拒绝处理入口）。
 */

package com.starrainnotes.analytics;

import com.starrainnotes.analytics.service.impl.AnalyticsAggregateServiceImpl;
import com.starrainnotes.analytics.api.ContentViewEvent;
import com.starrainnotes.analytics.utils.ReferrerClassifier;
import com.starrainnotes.analytics.entity.AnalyticsEventEntity;
import com.starrainnotes.analytics.exception.AnalyticsDateRangeInvalidException;
import com.starrainnotes.analytics.exception.AnalyticsRouteInvalidException;
import com.starrainnotes.analytics.mapper.AnalyticsMapper;
import com.starrainnotes.analytics.service.impl.AnalyticsServiceImpl;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 统计业务服务测试，属于访问统计模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class AnalyticsServiceTest {
    /**
     * 数据映射。
     * 协作类型：<code>AnalyticsMapper</code>。
     */
    private final AnalyticsMapper mapper = mock(AnalyticsMapper.class);
    /**
     * 业务服务。
     */
    private final AnalyticsServiceImpl service = new AnalyticsServiceImpl(mapper, new ReferrerClassifier());

    /**
     * 分页读取ViewOnlyAcceptsKnown公开路由集合。
     * 主要协作调用：<code>service.recordPageView</code>。
     */
    @Test
    void pageViewOnlyAcceptsKnownPublicRoutes() {
        assertThrows(AnalyticsRouteInvalidException.class, () -> service.recordPageView("/useradmin/dashboard", null));
        verifyNoInteractions(mapper);
        service.recordPageView("/blog", "https://www.google.com/search?q=secret");
        org.mockito.ArgumentCaptor<AnalyticsEventEntity> capture = org.mockito.ArgumentCaptor.forClass(AnalyticsEventEntity.class);
        verify(mapper).insert(capture.capture());
        assertEquals("SEARCH", capture.getValue().getReferrerCategory());
        assertEquals("www.google.com", capture.getValue().getReferrerHost());
        assertFalse(capture.getValue().getReferrerHost().contains("secret"));
    }

    /**
     * configured站点主机是否Internal处理入口。
     * 关键处理步骤调用 <code>ReflectionTestUtils.setField</code>、<code>classifier.classify</code>。
     */
    @Test
    void configuredSiteHostIsInternal() {
        ReferrerClassifier classifier = new ReferrerClassifier();
        ReflectionTestUtils.setField(classifier, "frontendOrigin", "https://notes.example.com");
        assertEquals("INTERNAL", classifier.classify("https://notes.example.com/post?private=1").getCategory());
        assertEquals("notes.example.com", classifier.classify("https://notes.example.com/post?private=1").getHost());
    }

    /**
     * 统计新增失败Does不Fail公开内容处理入口。
     * 主要协作调用：<code>service.recordContentView</code>。
     */
    @Test
    void analyticsInsertFailureDoesNotFailPublicContent() {
        doThrow(new IllegalStateException("database unavailable")).when(mapper).insert(any());
        assertDoesNotThrow(() -> service.recordContentView(new ContentViewEvent("BLOG", 42L, "/blog/posts/:slug")));
    }

    /**
     * 无效内容及日期范围Are已拒绝处理入口。
     * 主要协作调用：<code>service.recordContentView</code>、<code>service.trend</code>。
     */
    @Test
    void invalidContentAndDateRangeAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> service.recordContentView(new ContentViewEvent("BLOG", 0L, "/blog/posts/:slug")));
        assertThrows(AnalyticsDateRangeInvalidException.class,
            () -> service.trend(LocalDate.now().minusDays(400), LocalDate.now()));
    }

    /**
     * aggregateReplacesRatherThanIncrements日处理入口。
     * 关键处理步骤调用 <code>LocalDate.now</code>、<code>aggregate.aggregateDay</code>、<code>times</code>。
     */
    @Test
    void aggregateReplacesRatherThanIncrementsDay() {
        AnalyticsAggregateServiceImpl aggregate = new AnalyticsAggregateServiceImpl(mapper);
        LocalDate yesterday = LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1);
        aggregate.aggregateDay(yesterday);
        aggregate.aggregateDay(yesterday);
        verify(mapper, times(2)).deleteSiteDay(yesterday);
        verify(mapper, times(2)).deleteContentDay(yesterday);
        verify(mapper, times(2)).deleteReferrerDay(yesterday);
        verify(mapper, times(2)).insertSiteDay(yesterday);
        verify(mapper, times(2)).insertContentDay(yesterday);
        verify(mapper, times(2)).insertReferrerDay(yesterday);
    }
}
