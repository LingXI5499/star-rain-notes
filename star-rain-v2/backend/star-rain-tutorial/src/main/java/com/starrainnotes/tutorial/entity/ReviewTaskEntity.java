package com.starrainnotes.tutorial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sr_review_task")
public class ReviewTaskEntity {
    @TableId(type = IdType.AUTO)
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
