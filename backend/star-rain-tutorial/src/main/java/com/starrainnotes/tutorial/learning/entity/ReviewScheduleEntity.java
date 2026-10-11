package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 知识卡片复习计划实体。表名与列清单由 mapper/tutorial/ReviewScheduleMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class ReviewScheduleEntity {
    private Long id;
    private Long accountId;
    private Long knowledgeCardId;
    private Integer stepIndex;
    private Integer currentIntervalDays;
    private LocalDateTime nextReviewAt;
    private String status;
    private LocalDateTime lastResultAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
