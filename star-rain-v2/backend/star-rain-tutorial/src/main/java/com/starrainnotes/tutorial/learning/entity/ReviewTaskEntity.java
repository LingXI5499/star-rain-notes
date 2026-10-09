package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 复习任务实体。表名与列清单由 mapper/tutorial/ReviewTaskMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class ReviewTaskEntity {
    private Long id;
    private Long scheduleId;
    private Long accountId;
    private Long knowledgeCardId;
    private LocalDateTime dueAt;
    private String status;
    private LocalDateTime generatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
