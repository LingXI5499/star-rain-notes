/*
 * BlogPostAdminVO：博客模块的响应数据文件。
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
 * 后台文章列表项。
 *
 * 刻意不含正文：列表页只需要判断“有没有正文”，靠 hasBody 就够了，
 * 把整篇 Markdown 塞进分页响应会让后台列表变成最重的接口。
 */
/**
 * 博客文章后台响应对象，属于博客模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostAdminVO {

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
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    /**
     * 封面媒体资产标识。
     */
    private Long coverMediaAssetId;
    // 由 Media 模块给出，Blog 不自己拼媒体地址
    /**
     * 封面地址。
     */
    private String coverUrl;

    /**
     * 是否具备正文。
     */
    private boolean hasBody;

    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 已撤回时间。
     */
    private LocalDateTime withdrawnAt;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
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
