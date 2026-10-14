/*
 * WorkEntity：作品模块的持久化实体文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.portfolio.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作品实体。列名到属性名的映射由 application.yml 的 map-underscore-to-camel-case 完成，
 * 表名由 WorkMapper.xml 的语句写明。本模块 Mapper 已不继承 BaseMapper，
 * @TableName / @TableId 不会再生效，留着只会让人误以为框架仍在拼 SQL。
 */
/**
 * 作品实体，属于作品模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class WorkEntity {
    /** Optimistic editing version. */
    private Long rowVersion;
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
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
    private Long prototypeAssetId;
    /**
     * prototype入口。
     */
    private String prototypeEntry;
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
    private String projectStatus = "COMPLETED";
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
     * 初始值为 <code>false</code>。
     */
    private Boolean featured = false;
    /**
     * 展示排序值。
     * 初始值为 <code>0</code>。
     */
    private Integer sortOrder = 0;
    /**
     * SEO标题。
     */
    private String seoTitle;
    /**
     * SEO描述。
     */
    private String seoDescription;

    /**
     * Markdown 格式的正文。
     */
    private String bodyMarkdown;
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
