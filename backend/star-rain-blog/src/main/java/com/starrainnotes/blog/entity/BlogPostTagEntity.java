package com.starrainnotes.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/*
 * 文章与标签的绑定关系。
 *
 * 只有 created_at，没有 updated_at：绑定关系一旦建立就不需要修改，
 * 要改就是解除再加，这样“历史上是否绑定过”这件事不会被悄悄改写。
 */
@Data
@TableName("sr_blog_post_tag")
public class BlogPostTagEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long tagId;

    private LocalDateTime createdAt;
}
