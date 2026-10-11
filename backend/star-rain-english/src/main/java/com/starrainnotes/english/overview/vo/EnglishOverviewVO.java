/*
 * EnglishOverviewVO：英语学习模块的响应数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：EnglishOverviewEntity。
 * 阅读入口：from（从输入构造英语Overview）。
 */

package com.starrainnotes.english.overview.vo;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 英语总览的响应视图。
 *
 * 字段名就是前端 JSON 字段名，改类名/包名都无所谓，改字段名会静默破坏前端。
 */
/**
 * 英语Overview响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnglishOverviewVO {

    /**
     * 标题。
     */
    private String title;
    /**
     * 副标题。
     */
    private String subtitle;
    /**
     * 介绍。
     */
    private String introduction;
    /**
     * 当前Stage。
     */
    private String currentStage;
    /**
     * 路线Markdown。
     */
    private String roadmapMarkdown;

    /**
     * 从输入构造英语Overview。
     *
     * @param entity 数据库映射实体；类型为 <code>EnglishOverviewEntity</code>
     * @return 由输入对象组装的英语Overview响应对象
     */
    public static EnglishOverviewVO from(EnglishOverviewEntity entity) {
        return EnglishOverviewVO.builder()
                .title(entity.getTitle())
                .subtitle(entity.getSubtitle())
                .introduction(entity.getIntroduction())
                .currentStage(entity.getCurrentStage())
                .roadmapMarkdown(entity.getRoadmapMarkdown())
                .build();
    }
}
