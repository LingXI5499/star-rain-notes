/*
 * StudyPlanDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习计划传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanDTO {
    /**
     * 教程标识。
     * 不允许为 null。
     */
    @NotNull
    private Long tutorialId;
    /**
     * 名称。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 160)</code>。
     */
    @NotBlank
    @Size(max = 160)
    private String name;
    /**
     * scope类型。
     * 不能为空或仅含空白字符。
     */
    @NotBlank
    private String scopeType;
    /**
     * scope标识。
     * 不允许为 null。
     */
    @NotNull
    private Long scopeId;
    /**
     * 开始日期。
     * 不允许为 null。
     */
    @NotNull
    private LocalDate startDate;
    /**
     * 结束日期。
     */
    private LocalDate endDate;
    /**
     * 学习Weekdays。
     * 不允许为空。
     * 校验规则 <code>@Size(max = 7)</code>。
     */
    @NotEmpty
    @Size(max = 7)
    private List<@Min(1) @Max(7) Integer> studyWeekdays;
    /**
     * daily目标分钟。
     * 校验规则 <code>@Min(5)</code>。
     * 校验规则 <code>@Max(600)</code>。
     */
    @Min(5)
    @Max(600)
    private Integer dailyTargetMinutes;
}
