package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 章节知识卡片实体。表名与列清单由 mapper/tutorial/TutorialKnowledgeCardMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class TutorialKnowledgeCardEntity {
    private Long id;
    private Long chapterId;
    private Integer contentVersion = 1;
    private String frontText;
    private String backMarkdown;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
