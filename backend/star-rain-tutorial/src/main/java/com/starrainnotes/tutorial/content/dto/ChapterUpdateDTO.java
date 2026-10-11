/*
 * ChapterUpdateDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.content.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 章节更新传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterUpdateDTO {
    /**
     * 记录版本号，用于区分不同修订或并发更新结果。
     */
    private Long rowVersion;
    /**
     * 标题。
     * 校验规则 <code>@Size(max = 200)</code>。
     */
    @Size(max = 200)
    private String title;
    /**
     * 摘要。
     * 校验规则 <code>@Size(max = 1000)</code>。
     */
    @Size(max = 1000)
    private String summary;
    /**
     * Markdown 格式的正文。
     * 校验规则 <code>@Size(max = 2000000)</code>。
     */
    @Size(max = 2000000)
    private String bodyMarkdown;
}
