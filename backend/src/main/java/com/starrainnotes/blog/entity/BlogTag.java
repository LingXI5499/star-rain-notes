package com.starrainnotes.blog.entity;

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

/**
 * Flat blog tag (frozen table {@code blog_tag}).
 */
@TableName("blog_tag")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTag {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String slug;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

}
