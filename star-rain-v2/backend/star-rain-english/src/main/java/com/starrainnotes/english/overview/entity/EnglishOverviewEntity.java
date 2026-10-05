package com.starrainnotes.english.overview.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 英语概览实体。列名到属性名的映射由 application.yml 的 map-underscore-to-camel-case 完成，
 * 表名由 EnglishOverviewMapper.xml 的语句写明。本 Mapper 已不继承 BaseMapper，
 * @TableName / @TableId 不会再生效，留着只会让人误以为框架仍在拼 SQL。
 */
@Data
public class EnglishOverviewEntity {
    private Integer id;
    private String title;
    private String subtitle;
    private String introduction;
    private String currentStage;
    private String roadmapMarkdown;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
