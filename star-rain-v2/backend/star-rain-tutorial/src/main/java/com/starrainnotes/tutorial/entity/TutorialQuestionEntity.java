package com.starrainnotes.tutorial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_tutorial_question")
public class TutorialQuestionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long chapterId;
    private String questionText;
    private String referenceAnswer;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
