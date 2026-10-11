/*
 * BlogReferenceApi：博客模块的跨模块接口文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：BlogPostSummary。
 * 阅读入口：exists（判断是否存在博客引用）；isPublished（判断是否已发布）；summary（摘要处理入口）。
 */

package com.starrainnotes.blog.api;

import com.starrainnotes.blog.api.dto.BlogPostSummary;

/*
 * 博客被其它模块引用时的验证契约。
 *
 * Profile 的精选内容保存 blog postId 之前必须能确认它存在且已发布，
 * 但不允许直接读 Blog 的表。
 */
/**
 * 博客引用接口，属于博客模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface BlogReferenceApi {

    // 文章是否存在（不区分状态）
    /**
     * 判断是否存在博客引用。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean exists(Long postId);

    // 是否已发布：只有 true 才允许进入其它模块的公开展示区
    /**
     * 判断是否已发布。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean isPublished(Long postId);

    // 摘要，文章不存在返回 null（查询语义，不抛异常）
    /**
     * 摘要处理入口。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>BlogPostSummary</code> 的处理结果
     */
    BlogPostSummary summary(Long postId);
}
