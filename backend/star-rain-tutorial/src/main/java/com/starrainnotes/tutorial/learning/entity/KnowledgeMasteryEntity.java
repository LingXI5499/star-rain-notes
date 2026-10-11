package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 知识掌握状态实体。表名与列清单由 mapper/tutorial/KnowledgeMasteryMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class KnowledgeMasteryEntity {
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
