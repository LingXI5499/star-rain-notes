package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 章节实体。表名与列清单由 mapper/tutorial/TutorialChapterMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 *
 * summary 原先靠 @TableField(updateStrategy = FieldStrategy.ALWAYS) 保证
 * 「为 null 也写进 UPDATE」（清空摘要），这个语义已由 XML 的 updateContent 语句显式承担。
 */
@Data
public class TutorialChapterEntity {
    private Long id;
    private Long tutorialId;
    private Long groupId;
    private String slug;
    private String title;
    private String summary;
    private String bodyMarkdown;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
