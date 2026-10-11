/*
 * EnglishOverviewEntity：英语学习模块的持久化实体文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.english.overview.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 英语概览实体。列名到属性名的映射由 application.yml 的 map-underscore-to-camel-case 完成，
 * 表名由 EnglishOverviewMapper.xml 的语句写明。本 Mapper 已不继承 BaseMapper，
 * @TableName / @TableId 不会再生效，留着只会让人误以为框架仍在拼 SQL。
 */
/**
 * 英语Overview实体，属于英语学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class EnglishOverviewEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Integer id;
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
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
