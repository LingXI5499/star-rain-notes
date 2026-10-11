/*
 * TutorialDashboardStatsDTO：教程与学习模块的请求与传输数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.tutorial.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/* 教程概览的聚合查询结果，不作为 HTTP 响应直接返回。 */
/**
 * 教程概览统计字段传输对象，属于教程与学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialDashboardStatsDTO {
    /**
     * 符合查询条件的总记录数。
     */
    private long total;
    /**
     * drafts。
     */
    private long drafts;
    /**
     * 章节列表。
     */
    private long chapters;
    /**
     * 草稿章节列表。
     */
    private long draftChapters;
}
