package com.starrainnotes.tutorial.learning.entity;

import lombok.Data;
import java.time.LocalDateTime;

/*
 * 用户章节问题答案实体。表名与列清单由 mapper/tutorial/UserQuestionAnswerMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class UserQuestionAnswerEntity {
    private Long id;
    private Long accountId;
    private Long questionId;
    private String answerText;
    private LocalDateTime firstSubmittedAt;
    private LocalDateTime referenceUnlockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
