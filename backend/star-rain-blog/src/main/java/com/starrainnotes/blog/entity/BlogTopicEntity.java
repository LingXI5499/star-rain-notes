/*
 * BlogTopicEntity：博客模块的持久化实体文件。
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
 * 博客专题：人工策展的有序集合。
 *
 * name 允许重复、slug 唯一，与 Tag 的约束刻意不同：
 * 专题是“一组按顺序阅读的文章”，靠 slug 定位，名字只是说明。
 */
/**
 * 博客专题实体，属于博客模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_blog_topic")
public class BlogTopicEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 名称。
     */
    private String name;
    /**
     * 描述。
     */
    private String description;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 精选。
     */
    private Boolean featured;

    // ENABLED / DISABLED
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
