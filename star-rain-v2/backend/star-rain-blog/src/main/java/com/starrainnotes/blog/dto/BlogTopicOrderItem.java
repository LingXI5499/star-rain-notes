package com.starrainnotes.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 专题成员排序的批量更新项。
 *
 * 直接持有最终 sortOrder，而不是借助 CASE WHEN 拼 SQL：
 * MyBatis 的 foreach 批量 UPDATE 更容易读，也便于在测试里断言写入的序号。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTopicOrderItem {

    private Long postId;
    private Integer sortOrder;
}
