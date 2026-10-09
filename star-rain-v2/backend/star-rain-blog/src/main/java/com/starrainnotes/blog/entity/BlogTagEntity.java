package com.starrainnotes.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/*
 * 博客标签：多维分类，无序。
 *
 * name 与 slug 都唯一：标签是归档浏览的入口，同名不同 ID 会把同一批文章拆开。
 * 没有 sortOrder —— 这是它与 Topic 最本质的区别，需要顺序就该用 Topic。
 */
@Data
@TableName("sr_blog_tag")
public class BlogTagEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String slug;
    private String name;
    private String description;

    // ENABLED / DISABLED
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
