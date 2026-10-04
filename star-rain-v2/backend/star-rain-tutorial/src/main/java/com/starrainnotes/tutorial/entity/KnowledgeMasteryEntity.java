package com.starrainnotes.tutorial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sr_knowledge_mastery")
public class KnowledgeMasteryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private Long knowledgeCardId;
    private String systemSuggestedLevel;
    private String userSelfLevel;
    private String lastRecallRating;
    private Integer evidenceCount;
    private LocalDateTime firstLearnedAt;
    private LocalDateTime lastEvidenceAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
