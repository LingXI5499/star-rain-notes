package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 章节简答题实体。表名与列清单由 mapper/tutorial/TutorialQuestionMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class TutorialQuestionEntity {
    private Long id;
    private Long chapterId;
    private String questionText;
    private String referenceAnswer;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
