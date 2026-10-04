package com.starrainnotes.tutorial.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_tutorial_chapter")
public class TutorialChapterEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tutorialId;
    private Long groupId;
    private String slug;
    private String title;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;
    private String bodyMarkdown;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
