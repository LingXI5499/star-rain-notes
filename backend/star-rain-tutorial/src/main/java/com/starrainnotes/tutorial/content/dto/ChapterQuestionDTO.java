/*
 * ChapterQuestionDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 章节问题传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class ChapterQuestionDTO {
    /**
     * 记录版本号，用于区分不同修订或并发更新结果。
     */
    private Long rowVersion;
    /**
     * 问题文本。
     * 不能为空或仅含空白字符。
     */
    @NotBlank
    private String questionText;
    /**
     * 引用答案。
     * 不能为空或仅含空白字符。
     */
    @NotBlank
    private String referenceAnswer;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 知识卡片标识集合。
     */
    private java.util.List<Long> knowledgeCardIds = new java.util.ArrayList<>();
}
