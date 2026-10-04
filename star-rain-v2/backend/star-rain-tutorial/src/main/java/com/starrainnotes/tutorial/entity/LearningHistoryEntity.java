package com.starrainnotes.tutorial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sr_learning_history")
public class LearningHistoryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String eventType;
    private Long tutorialId;
    private Long groupId;
    private Long chapterId;
    private Long knowledgeCardId;
    private Long studyTaskId;
    private Long reviewTaskId;
    private String detailJson;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
