/*
 * AnswerVersionDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.learning.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
/**
 * 答案版本传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data public class AnswerVersionDTO {
 /**
  * 用户提交的回答文本。
  * 不能为空或仅含空白字符。
  * 校验规则 <code>@Size(max = 100000)</code>。
  */
 @NotBlank @Size(max=100000) private String answerText;
 /**
  * 答案Phase。
  * 不允许为 null。
  * 校验规则 <code>@Pattern(regexp = "BEFORE_REFERENCE|AFTER_REFERENCE")</code>。
  */
 @NotNull @Pattern(regexp="BEFORE_REFERENCE|AFTER_REFERENCE") private String answerPhase = "BEFORE_REFERENCE";
}
