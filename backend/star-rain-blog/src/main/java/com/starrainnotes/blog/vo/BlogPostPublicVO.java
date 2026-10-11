/*
 * BlogPostPublicVO：博客模块的响应数据文件。
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

/*
 * 前台文章列表项。
 *
 * 只暴露公开可展示的字段：没有 status、没有创建者账户 ID、没有正文。
 * 前台不需要知道文章处于哪个内部状态，只知道它已经发布。
 */
/**
 * 博客文章公开响应对象，属于博客模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostPublicVO {

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
     * 标签列表。
     */
    private List<BlogTagVO> tags;
    /**
     * 专题列表。
     */
    private List<BlogTopicVO> topics;
}
