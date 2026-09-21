package com.starrainnotes.portfolio.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Portfolio project (frozen table {@code portfolio_project}).
 *
 * <p>publish_status (DRAFT/PUBLISHED/WITHDRAWN) and project_status
 * (DEVELOPING/COMPLETED/ONLINE) are independent states. Cover uses
 * cover_media_id; gallery rows live in portfolio_project_media.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@TableName(value = "portfolio_project", autoResultMap = true)
public class PortfolioProject {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String role;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> techStack;
    private String bodyMarkdown;
    private Long coverMediaId;
    private String repositoryUrl;
    private String demoUrl;
    private String publishStatus;
    private String projectStatus;
    private Boolean featured;
    private Integer sortOrder;
    private LocalDate startedAt;
    private LocalDate completedAt;
    private String seoTitle;
    private String seoDescription;
    private LocalDateTime publishedAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
