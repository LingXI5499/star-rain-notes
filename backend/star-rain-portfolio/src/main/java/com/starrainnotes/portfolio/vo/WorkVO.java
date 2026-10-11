/*
 * WorkVO：作品模块的响应数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.portfolio.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作品响应对象，属于作品模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 作品类型。
     */
    private String workType;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * prototype资产标识。
     */
    private String prototypeAssetId;
    /**
     * prototype入口。
     */
    private String prototypeEntry;
    /**
     * prototype地址。
     */
    private String prototypeUrl;
    /**
     * 副标题。
     */
    private String subtitle;
    /**
     * 分类标识。
     */
    private Long categoryId;
    /**
     * 格式标识。
     */
    private Long formatId;
    /**
     * 角色。
     */
    private String role;
    /**
     * 技术技术栈。
     */
    private String techStack;
    /**
     * project状态。
     */
    private String projectStatus;
    /**
     * started触发。
     */
    private java.time.LocalDate startedOn;
    /**
     * ended触发。
     */
    private java.time.LocalDate endedOn;
    /**
     * 精选。
     */
    private Boolean featured;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * SEO标题。
     */
    private String seoTitle;
    /**
     * SEO描述。
     */
    private String seoDescription;
    /**
     * 分类。
     */
    private WorkTaxonomyVO category;
    /**
     * 格式。
     */
    private WorkTaxonomyVO format;
    /**
     * 标签列表。
     */
    private java.util.List<WorkTaxonomyVO> tags;
    /**
     * sections。
     */
    private java.util.List<WorkSectionVO> sections;
    /**
     * 可搜索文本。
     */
    private String searchableText;
    /**
     * 是否具备Sections。
     */
    private Boolean hasSections;
    /**
     * 上一项作品。
     */
    private WorkNavigationVO previousWork;
    /**
     * 下一项作品。
     */
    private WorkNavigationVO nextWork;

    /**
     * Markdown 格式的正文。
     */
    private String bodyMarkdown;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 类型详情。
     */
    private JsonNode typeDetail;
    /**
     * 媒体。
     */
    private List<WorkMediaVO> media;
    /**
     * links。
     */
    private List<WorkLinkVO> links;
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
}
