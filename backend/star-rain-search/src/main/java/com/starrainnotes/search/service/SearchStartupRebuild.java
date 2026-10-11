/*
 * SearchStartupRebuild：搜索模块的业务契约文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：SearchRebuildService、SearchDocumentMapper。
 * 阅读入口：seedEmptyIndex（seed空白索引处理入口）。
 */

package com.starrainnotes.search.service;

import com.starrainnotes.search.service.SearchRebuildService;

import com.starrainnotes.search.mapper.SearchDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 搜索StartupRebuild，属于搜索模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
@Component
@RequiredArgsConstructor
public class SearchStartupRebuild {
    /**
     * 当前类使用的日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(SearchStartupRebuild.class);
    /**
     * 数据映射。
     * 协作类型：<code>SearchDocumentMapper</code>。
     */
    private final SearchDocumentMapper mapper;
    /**
     * rebuild。
     * 协作类型：<code>SearchRebuildService</code>。
     */
    private final SearchRebuildService rebuild;

    /**
     * seed空白索引处理入口。
     * 调用约束：<code>@EventListener(ApplicationReadyEvent.class)</code>。
     * 主要协作调用：<code>mapper.activeCount</code>、<code>rebuild.rebuildAll</code>、<code>rebuild.rebuildType</code>。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedEmptyIndex() {
        try {
            if (mapper.activeCount() == 0) {
                rebuild.rebuildAll();
                log.info("Search index initialized with {} public documents", mapper.activeCount());
            } else {
                // Existing installations already have other modules indexed; backfill English too.
                rebuild.rebuildType("ENGLISH");
            }
        } catch (RuntimeException exception) {
            log.error("Search startup rebuild failed; admin rebuild remains available", exception);
        }
    }
}
