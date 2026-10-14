/*
 * BlogPostEntity：博客模块的持久化实体文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

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
/**
 * 博客文章实体，属于博客模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_blog_post")
public class BlogPostEntity {
    /** Optimistic editing version. */
    private Long rowVersion;

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    // 公开 URL 短名，全局唯一；发布后不建议再改，否则旧链接失效
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * Markdown 格式的正文。
     */
    private String bodyMarkdown;

    /**
     * 封面媒体资产标识。
     */
    private Long coverMediaAssetId;

    // DRAFT / PUBLISHED / WITHDRAWN
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    // 首次发布时间：恢复时不被覆盖，归档排序依赖它
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 已撤回时间。
     */
    private LocalDateTime withdrawnAt;

    /**
     * 创建该记录的账号标识。
     */
    private Long createdByAccountId;
    /**
     * 最近一次更新该记录的账号标识。
     */
    private Long updatedByAccountId;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
