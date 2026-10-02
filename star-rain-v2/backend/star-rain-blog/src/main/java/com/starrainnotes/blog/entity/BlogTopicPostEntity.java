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
@Data
@TableName("sr_blog_topic_post")
public class BlogTopicPostEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long topicId;
    private Long postId;

    private Integer sortOrder;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
