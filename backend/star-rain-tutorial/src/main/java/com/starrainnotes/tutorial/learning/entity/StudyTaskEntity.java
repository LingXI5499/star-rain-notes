package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * 学习任务实体。表名与列清单由 mapper/tutorial/StudyTaskMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class StudyTaskEntity {
    private Long id;
    private Long planId;
    private Long accountId;
    private Long tutorialId;
    private Long chapterId;
    private String targetType;
    private Long targetId;
    private LocalDate taskDate;
    private Integer sequenceNo;
    private String status;
    private Integer generationVersion;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
