package com.starrainnotes.tutorial.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sr_learning_progress")
public class LearningProgressEntity {
    @TableId(type = IdType.AUTO)
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
