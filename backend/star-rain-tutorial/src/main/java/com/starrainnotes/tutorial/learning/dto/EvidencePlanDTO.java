/*
 * EvidencePlanDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.learning.dto;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
/**
 * 证据计划传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class EvidencePlanDTO {
 /**
  * 教程标识。
  * 不允许为 null。
  */
 @NotNull private Long tutorialId;
 /**
  * 名称。
  * 不能为空或仅含空白字符。
  * 校验规则 <code>@Size(max = 160)</code>。
  */
 @NotBlank @Size(max=160) private String name;
 /**
  * entire教程。
  */
 private boolean entireTutorial;
 /**
  * 分组标识集合。
  * 校验规则 <code>@Size(max = 1000)</code>。
  */
 @Size(max=1000) private List<@NotNull Long> groupIds = new ArrayList<>();
 /**
  * 章节标识集合。
  * 校验规则 <code>@Size(max = 10000)</code>。
  */
 @Size(max=10000) private List<@NotNull Long> chapterIds = new ArrayList<>();
 /**
  * 目标卡片集合Per任务。
  * 校验规则 <code>@Min(5)</code>。
  * 校验规则 <code>@Max(30)</code>。
  * 初始值为 <code>12</code>。
  */
 @Min(5) @Max(30) private Integer targetCardsPerTask = 12;
}
