/*
 * WorkPatchDTO：作品模块的请求与传输数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.portfolio.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作品Patch传输对象，属于作品模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkPatchDTO {
    /**
     * 作品类型。
     */
    private String workType;
    /**
     * 公开固定链接的路径标识。
     * 校验规则 <code>@Size(max = 180)</code>。
     */
    @Size(max = 180)
    private String slug;
    /**
     * 标题。
     * 校验规则 <code>@Size(max = 255)</code>。
     */
    @Size(max = 255)
    private String title;
    /**
     * 摘要。
     * 校验规则 <code>@Size(max = 1000)</code>。
     */
    @Size(max = 1000)
    private String summary;
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
     * clearStarted触发。
     */
    private Boolean clearStartedOn;
    /**
     * clearEnded触发。
     */
    private Boolean clearEndedOn;
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
     * 标签标识集合。
     */
    private java.util.List<Long> tagIds;

}
