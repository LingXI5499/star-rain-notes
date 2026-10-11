/*
 * BlogPostTagEntity：博客模块的持久化实体文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

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
/**
 * 博客文章标签实体，属于博客模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_blog_post_tag")
public class BlogPostTagEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章标识。
     */
    private Long postId;
    /**
     * 标签标识。
     */
    private Long tagId;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
