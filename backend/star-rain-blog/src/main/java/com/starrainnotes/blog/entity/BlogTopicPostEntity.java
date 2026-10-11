/*
 * BlogTopicPostEntity：博客模块的持久化实体文件。
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
 * 专题成员关系，带人工顺序。
 *
 * sortOrder 从 1 开始连续编号：移除成员后会把后面的序号整体前移，
 * 这样“下一个序号 = 当前最大值 + 1”，不会因为长期增删产生巨大空洞。
 */
/**
 * 博客专题文章实体，属于博客模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_blog_topic_post")
public class BlogTopicPostEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 专题标识。
     */
    private Long topicId;
    /**
     * 文章标识。
     */
    private Long postId;

    /**
     * 展示排序值。
     */
    private Integer sortOrder;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
