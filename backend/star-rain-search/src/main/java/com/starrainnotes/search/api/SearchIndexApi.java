/*
 * SearchIndexApi：搜索模块的跨模块接口文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：SearchableDocument。
 * 阅读入口：upsert（新增或更新搜索索引）；remove（移除搜索索引）；removeByContent（移除依据内容）。
 */

package com.starrainnotes.search.api;

import com.starrainnotes.search.api.dto.SearchableDocument;

/**
 * 搜索索引接口，属于搜索模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface SearchIndexApi {
    /**
     * 新增或更新搜索索引。
     *
     * @param document 文档；类型为 <code>SearchableDocument</code>
     */
    void upsert(SearchableDocument document);
    /**
     * 移除搜索索引。
     *
     * @param documentKey 文档键；类型为 <code>String</code>
     */
    void remove(String documentKey);
    /**
     * 移除依据内容。
     *
     * @param contentType 内容类型或请求声明的 Content-Type；类型为 <code>String</code>
     * @param contentId 内容标识；类型为 <code>Long</code>
     */
    void removeByContent(String contentType, Long contentId);
}
