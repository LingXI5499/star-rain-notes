/*
 * BlogPostAdminDetailVO：博客模块的响应数据文件。
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
 * 后台文章详情 / 预览。
 *
 * contentMediaAssetIds 是从正文 Markdown 里解析出来的 blog.content 引用集合。
 * 之所以由后端解析后回给前端，是为了让“正文里有哪些媒体”只有一个真源（正文本身）：
 * 前端拿它渲染媒体面板，编辑正文时不需要另外维护一份 id 列表。
 */
/**
 * 博客文章后台详情响应对象，属于博客模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostAdminDetailVO {

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
     * 内容媒体资产标识集合。
     */
    private List<Long> contentMediaAssetIds;

    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
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
     * 创建该记录的账号标识。
     */
    private Long createdByAccountId;
    /**
     * 最近一次更新该记录的账号标识。
     */
    private Long updatedByAccountId;

    /**
     * 标签列表。
     */
    private List<BlogTagVO> tags;
    /**
     * 专题列表。
     */
    private List<BlogTopicVO> topics;

    /*
     * 是否含已停用标签。
     *
     * 已绑定后被停用的标签不影响发布（历史关系必须保留），但编辑器需要提示作者
     * “这篇还在用一个已经下架的标签”，否则前台分类入口消失时会找不到原因。
     */
    /**
     * 是否具备已禁用标签列表。
     */
    private boolean hasDisabledTags;
}
