/*
 * BlogSearchSourceApi：博客模块的跨模块接口文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：BlogDocumentPage、BlogPostDocument。
 * 阅读入口：listPublishedDocuments（列出已发布Documents）；getPublishedDocument（读取已发布文档）；getPublishedDocumentBySlug（读取已发布文档依据固定链接标识）。
 */

package com.starrainnotes.blog.api;

import com.starrainnotes.blog.api.dto.BlogDocumentPage;
import com.starrainnotes.blog.api.dto.BlogPostDocument;

/*
 * 博客公开文档源契约，供 Search / SEO 重建索引使用。
 *
 * 与 BlogPublicApi 的区别：这里返回可直接建索引的完整文档（正文、分类、时间），
 * 且用游标分页支持全量遍历。
 */
/**
 * 博客搜索来源接口，属于博客模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface BlogSearchSourceApi {

    // cursor 为 null 表示从头开始；返回的 nextCursor 为 null 表示已到末尾
    /**
     * 列出已发布Documents。
     *
     * @param cursor cursor；类型为 <code>Long</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 类型为 <code>BlogDocumentPage</code> 的处理结果
     */
    BlogDocumentPage listPublishedDocuments(Long cursor, int limit);

    // 单篇文档；未发布或不存在返回 null
    /**
     * 读取已发布文档。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostDocument</code> 的处理结果
     */
    BlogPostDocument getPublishedDocument(Long postId);

    /**
     * 读取已发布文档依据固定链接标识。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>BlogPostDocument</code> 的处理结果
     */
    BlogPostDocument getPublishedDocumentBySlug(String slug);
}
