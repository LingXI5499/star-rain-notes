/*
 * VocabularyReviewRequestDTO：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/*
 * 单词复习完成请求。
 *
 * reviewSessionId 由前端生成（UUID），是幂等键：同一个会话重放只记一次复习。
 */
/**
 * 词汇审核请求传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class VocabularyReviewRequestDTO {

    /**
     * 审核会话标识。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Pattern(regexp = "[0-9a-fA-F\\-]{36}", message = "\u590d\u4e60\u4f1a\u8bdd\u6807\u8bc6\u5fc5\u987b\u662f UUID")</code>。
     */
    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F\\-]{36}", message = "复习会话标识必须是 UUID")
    private String reviewSessionId;

    /**
     * direction。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Pattern(regexp = "EN_TO_ZH|ZH_TO_EN", message = "\u590d\u4e60\u65b9\u5411\u53ea\u80fd\u662f\u82f1\u8bd1\u4e2d\u6216\u4e2d\u8bd1\u82f1")</code>。
     */
    @NotBlank
    @Pattern(regexp = "EN_TO_ZH|ZH_TO_EN", message = "复习方向只能是英译中或中译英")
    private String direction;
}
