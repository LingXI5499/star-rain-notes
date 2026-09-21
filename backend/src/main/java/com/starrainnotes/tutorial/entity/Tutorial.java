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

/** Persistent row for the frozen {@code tutorial} table. */
@TableName("tutorial")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tutorial {
    @TableId(type = IdType.AUTO) private Long id;
    private Long categoryId;
    private String title;
    private String slug;
    private String summary;
    private Long coverMediaId;
    private String publishStatus;
    private Integer sortOrder;
    private String seoTitle;
    private String seoDescription;
    private LocalDateTime publishedAt;
    @TableField(updateStrategy = FieldStrategy.NEVER) private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER) private LocalDateTime updatedAt;
}
