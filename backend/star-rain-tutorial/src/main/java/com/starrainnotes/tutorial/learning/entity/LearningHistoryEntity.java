package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 个人学习事件历史实体。表名与列清单由 mapper/tutorial/LearningHistoryMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class LearningHistoryEntity {
    private Long id;
    private Long accountId;
    private String eventType;
    private Long tutorialId;
    private Long groupId;
    private Long chapterId;
    private Long knowledgeCardId;
    private Long studyTaskId;
    private Long reviewTaskId;
    private String detailJson;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
