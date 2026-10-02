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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostAdminDetailVO {

    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String bodyMarkdown;

    private Long coverMediaAssetId;
    private String coverUrl;

    private List<Long> contentMediaAssetIds;

    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdByAccountId;
    private Long updatedByAccountId;

    private List<BlogTagVO> tags;
    private List<BlogTopicVO> topics;

    /*
     * 是否含已停用标签。
     *
     * 已绑定后被停用的标签不影响发布（历史关系必须保留），但编辑器需要提示作者
     * “这篇还在用一个已经下架的标签”，否则前台分类入口消失时会找不到原因。
     */
    private boolean hasDisabledTags;
}
