/*
 * VocabularyDisplayRequestDTO：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/*
 * 单卡显示模式写入请求。跟随全局用 DELETE /display 清除，不在这里传 FOLLOW_GLOBAL。
 */
/**
 * 词汇显示请求传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class VocabularyDisplayRequestDTO {

    /**
     * 显示Mode。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Pattern(regexp = "BILINGUAL|ENGLISH_ONLY|CHINESE_ONLY", message = "\u663e\u793a\u6a21\u5f0f\u53ea\u80fd\u662f\u53cc\u8bed\u3001\u53ea\u770b\u82f1\u6587\u6216\u53ea\u770b\u4e2d\u6587")</code>。
     */
    @NotBlank
    @Pattern(regexp = "BILINGUAL|ENGLISH_ONLY|CHINESE_ONLY", message = "显示模式只能是双语、只看英文或只看中文")
    private String displayMode;
}
