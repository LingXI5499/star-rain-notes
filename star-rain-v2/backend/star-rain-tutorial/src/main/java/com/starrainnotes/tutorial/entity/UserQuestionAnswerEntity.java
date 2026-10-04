package com.starrainnotes.tutorial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sr_user_question_answer")
public class UserQuestionAnswerEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private Long questionId;
    private String answerText;
    private LocalDateTime firstSubmittedAt;
    private LocalDateTime referenceUnlockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
