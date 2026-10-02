package com.starrainnotes.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 文章专题投影行，同样用于批量查询后分组，避免 N+1
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostTopicRow {

    private Long postId;
    private Long topicId;
    private String slug;
    private String name;
    private Integer sortOrder;
}
