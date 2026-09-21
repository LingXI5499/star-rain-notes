package com.starrainnotes.tutorial.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Persistent row for the frozen {@code tutorial_category} table. */
@TableName("tutorial_category")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialCategory {
    @TableId(type = IdType.AUTO) private Long id;
    private Long parentId;
    private String name;
    private String slug;
    private Integer sortOrder;
    @TableField(updateStrategy = FieldStrategy.NEVER) private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER) private LocalDateTime updatedAt;
}
