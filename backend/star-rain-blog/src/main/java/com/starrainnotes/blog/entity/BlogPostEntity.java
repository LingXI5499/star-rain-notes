package com.starrainnotes.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/*
 * 博客文章。
 *
 * bodyMarkdown 与元数据（title/slug/summary/cover）分开更新：
 * 元数据走 updatePostMeta，正文走 updatePostBody，避免每次改标题都把整篇正文重写一遍。
 * coverMediaAssetId 只是逻辑引用，真正的跨模块引用登记在 sr_media_reference，
 * 由 Blog Service 通过 MediaReferenceApi 维护。
 */
@Data
@TableName("sr_blog_post")
public class BlogPostEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 公开 URL 短名，全局唯一；发布后不建议再改，否则旧链接失效
    private String slug;
    private String title;
    private String summary;
    private String bodyMarkdown;

    private Long coverMediaAssetId;

    // DRAFT / PUBLISHED / WITHDRAWN
    private String status;

    // 首次发布时间：恢复时不被覆盖，归档排序依赖它
    private LocalDateTime publishedAt;
    private LocalDateTime withdrawnAt;

    private Long createdByAccountId;
    private Long updatedByAccountId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
