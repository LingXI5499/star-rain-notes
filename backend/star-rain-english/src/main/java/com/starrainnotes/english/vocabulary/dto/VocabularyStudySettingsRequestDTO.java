/*
 * VocabularyStudySettingsRequestDTO：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.vocabulary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/*
 * 学习设置写入请求。MIXED 仅用于旧客户端兼容，写入时映射为 EN_TO_ZH。
 */
/**
 * 词汇学习Settings请求传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class VocabularyStudySettingsRequestDTO {

    /**
     * show英语。
     */
    private boolean showEnglish;

    /**
     * showChinese。
     */
    private boolean showChinese;

    /**
     * 审核Direction。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Pattern(regexp = "EN_TO_ZH|ZH_TO_EN|AUDIO_TO_BOTH|MIXED", message = "\u590d\u4e60\u65b9\u5411\u53ea\u80fd\u662f\u82f1\u8bd1\u4e2d\u3001\u4e2d\u8bd1\u82f1\u6216\u542c\u97f3\u8fa8\u8bcd")</code>。
     */
    @NotBlank
    @Pattern(regexp = "EN_TO_ZH|ZH_TO_EN|AUDIO_TO_BOTH|MIXED", message = "复习方向只能是英译中、中译英或听音辨词")
    private String reviewDirection;

    /**
     * dailyNew上限。
     * 校验规则 <code>@Min(0)</code>。
     * 校验规则 <code>@Max(200)</code>。
     */
    @Min(0)
    @Max(200)
    private int dailyNewLimit;

    /**
     * daily审核上限。
     * 校验规则 <code>@Min(1)</code>。
     * 校验规则 <code>@Max(1000)</code>。
     */
    @Min(1)
    @Max(1000)
    private int dailyReviewLimit;
}
