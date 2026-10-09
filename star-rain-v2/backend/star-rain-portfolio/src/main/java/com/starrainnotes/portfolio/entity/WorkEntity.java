package com.starrainnotes.portfolio.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作品实体。列名到属性名的映射由 application.yml 的 map-underscore-to-camel-case 完成，
 * 表名由 WorkMapper.xml 的语句写明。本模块 Mapper 已不继承 BaseMapper，
 * @TableName / @TableId 不会再生效，留着只会让人误以为框架仍在拼 SQL。
 */
@Data
public class WorkEntity {
    private Long id;
    private String slug;
    private String workType;
    private String title;
    private String summary;
    private Long prototypeAssetId;
    private String prototypeEntry;
    private String subtitle;
    private Long categoryId;
    private Long formatId;
    private String role;
    private String techStack;
    private String projectStatus = "COMPLETED";
    private java.time.LocalDate startedOn;
    private java.time.LocalDate endedOn;
    private Boolean featured = false;
    private Integer sortOrder = 0;
    private String seoTitle;
    private String seoDescription;

    private String bodyMarkdown;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime withdrawnAt;
    private Long createdByAccountId;
    private Long updatedByAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
