package com.starrainnotes.blog.api;

import com.starrainnotes.blog.api.dto.BlogPostSummary;

/*
 * 博客被其它模块引用时的验证契约。
 *
 * Profile 的精选内容保存 blog postId 之前必须能确认它存在且已发布，
 * 但不允许直接读 Blog 的表。
 */
public interface BlogReferenceApi {

    // 文章是否存在（不区分状态）
    boolean exists(Long postId);

    // 是否已发布：只有 true 才允许进入其它模块的公开展示区
    boolean isPublished(Long postId);

    // 摘要，文章不存在返回 null（查询语义，不抛异常）
    BlogPostSummary summary(Long postId);
}
