package com.starrainnotes.tutorial.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sr_review_result")
public class ReviewResultEntity {
    @TableId(type = IdType.AUTO)
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
