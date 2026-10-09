package com.starrainnotes.blog.api;

import com.starrainnotes.blog.api.dto.BlogDocumentPage;
import com.starrainnotes.blog.api.dto.BlogPostDocument;

/*
 * 博客公开文档源契约，供 Search / SEO 重建索引使用。
 *
 * 与 BlogPublicApi 的区别：这里返回可直接建索引的完整文档（正文、分类、时间），
 * 且用游标分页支持全量遍历。
 */
public interface BlogSearchSourceApi {

    // cursor 为 null 表示从头开始；返回的 nextCursor 为 null 表示已到末尾
    BlogDocumentPage listPublishedDocuments(Long cursor, int limit);

    // 单篇文档；未发布或不存在返回 null
    BlogPostDocument getPublishedDocument(Long postId);

    BlogPostDocument getPublishedDocumentBySlug(String slug);
}
