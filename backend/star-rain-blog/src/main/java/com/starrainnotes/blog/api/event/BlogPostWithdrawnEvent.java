/*
 * BlogPostWithdrawnEvent：博客模块的跨模块接口文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.blog.api.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 文章撤回事件（BLOG-009）：Search 删索引、SEO 移除快照与 Sitemap 项
/**
 * 博客文章已撤回事件，属于博客模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostWithdrawnEvent {

    /**
     * 文章标识。
     */
    private Long postId;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 已撤回时间。
     */
    private LocalDateTime withdrawnAt;
}
