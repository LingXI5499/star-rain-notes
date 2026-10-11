/*
 * EnglishOverviewRequestDTO：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.overview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 英语总览更新请求体。
 *
 * 字段名就是前端 JSON 字段名，改类名/包名都无所谓，改字段名会静默破坏前端。
 */
/**
 * 英语Overview请求传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnglishOverviewRequestDTO {

    /**
     * 标题。
     * 不能为空或仅含空白字符。
     * 校验规则 <code>@Size(max = 200)</code>。
     */
    @NotBlank
    @Size(max = 200)
    private String title;

    /**
     * 副标题。
     * 校验规则 <code>@Size(max = 500)</code>。
     */
    @Size(max = 500)
    private String subtitle;

    /**
     * 介绍。
     * 校验规则 <code>@Size(max = 10000)</code>。
     */
    @Size(max = 10000)
    private String introduction;

    /**
     * 路线Markdown。
     * 校验规则 <code>@Size(max = 200000)</code>。
     */
    @Size(max = 200000)
    private String roadmapMarkdown;
}
