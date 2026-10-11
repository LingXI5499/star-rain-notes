/*
 * TutorialReviewVO：教程与学习模块的响应数据文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：ReviewTargetView。
 * 阅读入口：viewType（view类型处理入口）。
 */

package com.starrainnotes.tutorial.content.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.starrainnotes.review.api.ReviewTargetView;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 教程审核响应对象，属于教程与学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@AllArgsConstructor
public class TutorialReviewVO implements ReviewTargetView {
    /**
     * 版本或修订的引用值。
     */
    private String revisionRef;
    /**
     * 标题。
     */
    private String title;
    /**
     * 内容快照。
     */
    private JsonNode contentSnapshot;

    /**
     * view类型处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    @JsonProperty("viewType")
    public String viewType() {
        return "TUTORIAL_SNAPSHOT";
    }
}
