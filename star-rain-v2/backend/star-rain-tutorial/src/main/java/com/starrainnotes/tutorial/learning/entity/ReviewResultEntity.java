package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 单次复习结果实体。表名与列清单由 mapper/tutorial/ReviewResultMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class ReviewResultEntity {
    private Long id;
    private Long reviewTaskId;
    private Long scheduleId;
    private Long accountId;
    private Long knowledgeCardId;
    private String recallRating;
    private Integer previousIntervalDays;
    private Integer nextIntervalDays;
    private LocalDateTime previousNextReviewAt;
    private LocalDateTime calculatedNextReviewAt;
    private LocalDateTime createdAt;
}
