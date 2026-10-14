/*
 * LearningProgressDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.learning.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习进度传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgressDTO {
    private Long expectedVersion;
    @Size(max = 64)
    private String eventId;
    /**
     * scrollAnchor。
     * 校验规则 <code>@Size(max = 255)</code>。
     */
    @Size(max = 255)
    private String scrollAnchor;
    /**
     * 进度比例。
     * 校验规则 <code>@DecimalMin("0.0")</code>。
     * 校验规则 <code>@DecimalMax("1.0")</code>。
     */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal progressRatio;
    /**
     * 学习秒Delta。
     * 校验规则 <code>@Min(0)</code>。
     * 校验规则 <code>@Max(1800)</code>。
     */
    @Min(0)
    @Max(1800)
    private Integer studySecondsDelta;
}
