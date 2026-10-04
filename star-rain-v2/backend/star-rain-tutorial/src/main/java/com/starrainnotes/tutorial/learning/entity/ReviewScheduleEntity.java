package com.starrainnotes.tutorial.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sr_review_schedule")
public class ReviewScheduleEntity {
    @TableId(type = IdType.AUTO)
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
