package com.starrainnotes.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/*
 * 博客专题：人工策展的有序集合。
 *
 * name 允许重复、slug 唯一，与 Tag 的约束刻意不同：
 * 专题是“一组按顺序阅读的文章”，靠 slug 定位，名字只是说明。
 */
@Data
@TableName("sr_blog_topic")
public class BlogTopicEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String slug;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean featured;

    // ENABLED / DISABLED
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
