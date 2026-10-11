/*
 * BlogPostPublicDetailVO：博客模块的响应数据文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.blog.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 前台文章详情：在列表项之上补充正文与最后更新时间
/**
 * 博客文章公开详情响应对象，属于博客模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostPublicDetailVO {

    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
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
    /**
     * 封面地址。
     */
    private String coverUrl;

    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 上一项。
     */
    private BlogPostNeighborVO previous;
    /**
     * 下一项。
     */
    private BlogPostNeighborVO next;

    /**
     * 标签列表。
     */
    private List<BlogTagVO> tags;
    /**
     * 专题列表。
     */
    private List<BlogTopicVO> topics;
}
