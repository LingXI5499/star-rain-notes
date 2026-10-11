/*
 * KnowledgeCardDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识卡片传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class KnowledgeCardDTO {
    /**
     * 记录版本号，用于区分不同修订或并发更新结果。
     */
    private Long rowVersion;
    /**
     * 知识卡片正面的提示文本。
     * 不能为空或仅含空白字符。
     */
    @NotBlank
    private String frontText;
    /**
     * 知识卡片背面的 Markdown 内容。
     * 不能为空或仅含空白字符。
     */
    @NotBlank
    private String backMarkdown;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
}
