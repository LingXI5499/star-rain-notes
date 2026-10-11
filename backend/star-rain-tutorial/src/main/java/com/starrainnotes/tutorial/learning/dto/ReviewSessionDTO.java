/*
 * ReviewSessionDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.learning.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
/**
 * 审核会话传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data public class ReviewSessionDTO {
 /**
  * mode。
  * 不允许为 null。
  * 校验规则 <code>@Pattern(regexp = "RECOMMENDED|STABLE_AUDIT")</code>。
  */
 @NotNull @Pattern(regexp="RECOMMENDED|STABLE_AUDIT") private String mode = "RECOMMENDED";
 /**
  * 数量。
  * 不允许为 null。
  * 校验规则 <code>@Min(1)</code>。
  * 校验规则 <code>@Max(100)</code>。
  * 初始值为 <code>10</code>。
  */
 @NotNull @Min(1) @Max(100) private Integer count = 10;
}
