/*
 * EvidenceRatingDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.learning.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
/**
 * 证据评分传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data public class EvidenceRatingDTO {
 /**
  * 评分。
  * 不允许为 null。
  * 校验规则 <code>@Pattern(regexp = "FORGOT|FUZZY|REMEMBERED")</code>。
  */
 @NotNull @Pattern(regexp="FORGOT|FUZZY|REMEMBERED") private String rating;
}
