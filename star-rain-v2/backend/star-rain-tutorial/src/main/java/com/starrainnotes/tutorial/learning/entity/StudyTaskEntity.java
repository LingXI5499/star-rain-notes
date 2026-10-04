package com.starrainnotes.tutorial.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sr_study_task")
public class StudyTaskEntity {
    @TableId(type = IdType.AUTO)
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
