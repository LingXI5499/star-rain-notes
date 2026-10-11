/*
 * SeoStartupRebuild：公开页面与 SEO模块的模块支持文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：SeoPageMapper、SeoRefreshApi、BlogTopicSeoSourceProvider、SeoRebuildService。
 * 阅读入口：seedEmptySnapshots（seed空白Snapshots处理入口）。
 */

package com.starrainnotes.seo.lifecycle;

import com.starrainnotes.seo.mapper.SeoPageMapper;
import com.starrainnotes.seo.api.SeoRefreshApi;
import com.starrainnotes.seo.provider.impl.BlogTopicSeoSourceProvider;
import com.starrainnotes.seo.service.SeoRebuildService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/*
 * 启动时给空库补一份 SEO 快照。
 *
 * 它监听 ApplicationReadyEvent，属于应用生命周期，不是业务领域事件。
 *
 * 名字保留 Startup 前缀，是为了让「只在启动时跑一次」这件事在类名上就能看出来。
 */
/**
 * SEOStartupRebuild，属于公开页面与 SEO模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
@RequiredArgsConstructor
public class SeoStartupRebuild {
    /**
     * 当前类使用的日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(SeoStartupRebuild.class);
    /**
     * 数据映射。
     * 协作类型：<code>SeoPageMapper</code>。
     */
    private final SeoPageMapper mapper;
    /**
     * rebuild。
     * 协作类型：<code>SeoRebuildService</code>。
     */
    private final SeoRebuildService rebuild;
    /**
     * 刷新。
     * 协作类型：<code>SeoRefreshApi</code>。
     */
    private final SeoRefreshApi refresh;
    /**
     * 博客专题列表。
     */
    private final BlogTopicSeoSourceProvider blogTopics;

    /**
     * seed空白Snapshots处理入口。
     * 调用约束：<code>@EventListener(ApplicationReadyEvent.class)</code>。
     * 主要协作调用：<code>mapper.activeCount</code>、<code>rebuild.rebuildAll</code>、<code>mapper.activePaths</code>、<code>blogTopics.listPublished</code>、<code>refresh.refreshRoute</code>。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedEmptySnapshots() {
        try {
            if (mapper.activeCount() == 0) {
                rebuild.rebuildAll();
                log.info("SEO snapshots initialized with {} public pages", mapper.activeCount());
            } else {
                // Existing installations already have snapshots, but lacked topic pages before this release.
                java.util.Set<String> activePaths = new java.util.HashSet<>(mapper.activePaths());
                for (var topic : blogTopics.listPublished()) {
                    if (!activePaths.contains(topic.getRoutePath())) {
                        refresh.refreshRoute(topic.getRoutePath());
                    }
                }
            }
        } catch (RuntimeException exception) {
            log.error("SEO startup rebuild failed; admin rebuild remains available", exception);
        }
    }
}
