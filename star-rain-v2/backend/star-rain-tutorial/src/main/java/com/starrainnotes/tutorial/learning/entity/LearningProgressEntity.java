package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * 章节学习进度实体。表名与列清单由 mapper/tutorial/LearningProgressMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class LearningProgressEntity {
    private Long id;
    private Long accountId;
    private Long tutorialId;
    private Long groupId;
    private Long chapterId;
    private String scrollAnchor;
    private BigDecimal progressRatio;
    private Long studySecondsTotal;
    private LocalDateTime completedAt;
    private LocalDateTime lastStudiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
